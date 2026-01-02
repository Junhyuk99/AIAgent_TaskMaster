package com.aiagent.service;

import com.aiagent.connector.LlmConnector;
import com.aiagent.dto.chat.AgentChatRequest;
import com.aiagent.dto.chat.AgentChatResponse;
import com.aiagent.dto.rag.RagContext;
import com.aiagent.entity.Agent;
import com.aiagent.entity.Conversation;
import com.aiagent.entity.Function;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.entity.Message;
import com.aiagent.executor.FunctionExecutionResult;
import com.aiagent.function.FunctionCall;
import com.aiagent.function.FunctionCallResult;
import com.aiagent.function.FunctionSchemaConverter;
import com.aiagent.function.LlmChatResult;
import com.aiagent.repository.AgentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentChatService {

    private final AgentRepository agentRepository;
    private final LlmServerService llmServerService;
    private final RagService ragService;
    private final FunctionSchemaConverter functionSchemaConverter;
    private final FunctionService functionService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @Value("${agent.function-calling.max-iterations:5}")
    private int maxFunctionCallIterations;

    public AgentChatResponse chat(Long agentId, Long userId, AgentChatRequest request) {
        Agent agent = getAgentWithAccess(agentId, userId);
        validateAgentConfiguration(agent);

        LlmConnector connector = llmServerService.getConnector(agent.getLlmServer().getId(), userId);

        // Build conversation context with RAG if knowledge bases are available
        String userMessage = buildUserMessage(request);
        RagContext ragContext = buildRagContext(agent, request.getMessage());
        String systemPrompt = ragContext.getAugmentedSystemPrompt();

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = UUID.randomUUID().toString();
        }

        // Get or create conversation
        Conversation conversation = conversationService.getOrCreateConversation(
                conversationId, agentId, userId
        );
        conversationId = conversation.getExternalId();

        // Save user message
        long startTime = System.currentTimeMillis();
        conversationService.addMessage(conversation, Message.Role.USER, request.getMessage(),
                null, null, null, null);

        // Update title if first message
        if (conversation.getMessageCount() == 1) {
            String title = conversationService.generateTitle(request.getMessage());
            conversation.setTitle(title);
        }

        // Get tools (functions) for the agent
        Set<Function> functions = agent.getFunctions();
        List<Map<String, Object>> tools = functionSchemaConverter.convertToTools(functions);
        boolean hasFunctions = !tools.isEmpty();

        String response;
        List<AgentChatResponse.FunctionExecution> functionExecutions = new ArrayList<>();

        if (hasFunctions) {
            // Use function calling flow
            response = chatWithFunctionCalling(
                    connector, agent, userId, systemPrompt, userMessage, tools, functions, functionExecutions
            );
        } else {
            // Regular chat without function calling
            response = connector.chat(
                    agent.getModelName(),
                    systemPrompt,
                    userMessage,
                    agent.getTemperature(),
                    agent.getMaxTokens()
            );
        }

        // Build sources list for response
        List<AgentChatResponse.DocumentSource> sources = null;
        if (ragContext.isContextFound() && ragContext.getSources() != null) {
            sources = ragContext.getSources().stream()
                    .map(s -> AgentChatResponse.DocumentSource.builder()
                            .documentId(s.getDocumentId())
                            .documentName(s.getDocumentName())
                            .score(s.getScore())
                            .build())
                    .collect(Collectors.toList());
        }

        long executionTimeMs = System.currentTimeMillis() - startTime;

        // Save assistant message
        conversationService.addMessage(conversation, Message.Role.ASSISTANT, response,
                agent.getModelName(), executionTimeMs, functionExecutions, sources);

        log.info("Agent {} chat completed for user {}, RAG context: {}, functions executed: {}",
                agentId, userId, ragContext.isContextFound(), functionExecutions.size());

        return AgentChatResponse.builder()
                .conversationId(conversationId)
                .response(response)
                .model(agent.getModelName())
                .agentId(agent.getId())
                .agentName(agent.getName())
                .sources(sources)
                .functionExecutions(functionExecutions.isEmpty() ? null : functionExecutions)
                .build();
    }

    /**
     * Handle chat with function calling loop.
     */
    private String chatWithFunctionCalling(
            LlmConnector connector,
            Agent agent,
            Long userId,
            String systemPrompt,
            String userMessage,
            List<Map<String, Object>> tools,
            Set<Function> functions,
            List<AgentChatResponse.FunctionExecution> functionExecutions) {

        // Initial chat with tools
        LlmChatResult result = connector.chatWithTools(
                agent.getModelName(),
                systemPrompt,
                userMessage,
                agent.getTemperature(),
                agent.getMaxTokens(),
                tools
        );

        // If no function calls, return the response
        if (!result.hasFunctionCalls()) {
            return result.getContent();
        }

        // Build conversation history for subsequent calls
        List<Map<String, Object>> conversationHistory = new ArrayList<>();

        // Add original user message
        conversationHistory.add(Map.of("role", "user", "content", userMessage));

        int iterations = 0;
        while (result.hasFunctionCalls() && iterations < maxFunctionCallIterations) {
            iterations++;

            // Execute function calls
            List<FunctionCallResult> results = executeFunctionCalls(
                    result.getFunctionCalls(), functions, userId
            );

            // Add assistant message with tool calls
            conversationHistory.add(buildAssistantToolCallMessage(result.getFunctionCalls()));

            // Add tool response messages
            for (FunctionCallResult fcResult : results) {
                conversationHistory.add(buildToolResponseMessage(fcResult));

                // Record function execution for response
                functionExecutions.add(AgentChatResponse.FunctionExecution.builder()
                        .functionName(fcResult.getFunctionCall().getName())
                        .arguments(fcResult.getFunctionCall().getArguments())
                        .result(fcResult.isSuccess() ? fcResult.getResult() : null)
                        .error(fcResult.getError())
                        .executionTimeMs(fcResult.getExecutionTimeMs())
                        .build());
            }

            // Continue conversation with function results
            result = connector.continueWithFunctionResult(
                    agent.getModelName(),
                    systemPrompt,
                    conversationHistory,
                    agent.getTemperature(),
                    agent.getMaxTokens(),
                    tools
            );
        }

        if (iterations >= maxFunctionCallIterations && result.hasFunctionCalls()) {
            log.warn("Max function call iterations reached for agent {}", agent.getId());
        }

        return result.getContent() != null ? result.getContent() : "";
    }

    /**
     * Execute a list of function calls.
     */
    private List<FunctionCallResult> executeFunctionCalls(
            List<FunctionCall> functionCalls,
            Set<Function> functions,
            Long userId) {

        List<FunctionCallResult> results = new ArrayList<>();

        for (FunctionCall call : functionCalls) {
            long startTime = System.currentTimeMillis();

            try {
                // Find the function by name
                Optional<Function> functionOpt = functionSchemaConverter.findFunctionByName(functions, call.getName());

                if (functionOpt.isEmpty()) {
                    results.add(FunctionCallResult.failure(
                            call, "Function not found: " + call.getName(), System.currentTimeMillis() - startTime));
                    continue;
                }

                Function function = functionOpt.get();

                // Execute the function
                FunctionExecutionResult execResult = functionService.executeFunction(function, call.getArguments());

                if (execResult.isSuccess()) {
                    String resultJson = serializeResult(execResult.getData());
                    results.add(FunctionCallResult.success(call, resultJson, execResult.getExecutionTimeMs()));
                } else {
                    results.add(FunctionCallResult.failure(call, execResult.getError(), execResult.getExecutionTimeMs()));
                }

            } catch (Exception e) {
                log.error("Function execution failed for {}: {}", call.getName(), e.getMessage());
                results.add(FunctionCallResult.failure(call, e.getMessage(), System.currentTimeMillis() - startTime));
            }
        }

        return results;
    }

    /**
     * Build assistant message with tool calls.
     */
    private Map<String, Object> buildAssistantToolCallMessage(List<FunctionCall> calls) {
        List<Map<String, Object>> toolCalls = new ArrayList<>();

        for (FunctionCall call : calls) {
            Map<String, Object> toolCall = new LinkedHashMap<>();
            toolCall.put("id", call.getId() != null ? call.getId() : UUID.randomUUID().toString());
            toolCall.put("type", "function");
            toolCall.put("function", Map.of(
                    "name", call.getName(),
                    "arguments", call.getArguments()
            ));
            toolCalls.add(toolCall);
        }

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "assistant");
        message.put("content", "");
        message.put("tool_calls", toolCalls);
        return message;
    }

    /**
     * Build tool response message.
     */
    private Map<String, Object> buildToolResponseMessage(FunctionCallResult result) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", result.getFunctionCall().getId() != null ?
                result.getFunctionCall().getId() : UUID.randomUUID().toString());
        message.put("content", result.getContentForLlm());
        return message;
    }

    /**
     * Serialize function result to JSON string.
     */
    private String serializeResult(Object data) {
        if (data == null) return "null";
        if (data instanceof String) return (String) data;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            return String.valueOf(data);
        }
    }

    public Flux<String> chatStream(Long agentId, Long userId, AgentChatRequest request) {
        Agent agent = getAgentWithAccess(agentId, userId);
        validateAgentConfiguration(agent);

        LlmConnector connector = llmServerService.getConnector(agent.getLlmServer().getId(), userId);

        // Build conversation context with RAG if knowledge bases are available
        String userMessage = buildUserMessage(request);
        RagContext ragContext = buildRagContext(agent, request.getMessage());
        String systemPrompt = ragContext.getAugmentedSystemPrompt();

        log.info("Agent {} streaming chat started for user {}, RAG context found: {}",
                agentId, userId, ragContext.isContextFound());

        return connector.chatStream(
                agent.getModelName(),
                systemPrompt,
                userMessage,
                agent.getTemperature(),
                agent.getMaxTokens()
        );
    }

    private Agent getAgentWithAccess(Long agentId, Long userId) {
        return agentRepository.findByIdWithAllRelations(agentId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Agent not found or access denied"));
    }

    private void validateAgentConfiguration(Agent agent) {
        if (agent.getLlmServer() == null) {
            throw new RuntimeException("Agent has no LLM server configured");
        }
        if (agent.getModelName() == null || agent.getModelName().isEmpty()) {
            throw new RuntimeException("Agent has no model configured");
        }
        if (!agent.getIsActive()) {
            throw new RuntimeException("Agent is not active");
        }
    }

    private RagContext buildRagContext(Agent agent, String query) {
        String baseSystemPrompt = buildBaseSystemPrompt(agent);
        Set<KnowledgeBase> knowledgeBases = agent.getKnowledgeBases();

        // If no knowledge bases, return base prompt
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return RagContext.builder()
                    .originalSystemPrompt(baseSystemPrompt)
                    .augmentedSystemPrompt(baseSystemPrompt)
                    .contextFound(false)
                    .build();
        }

        // Filter to only active knowledge bases
        Set<KnowledgeBase> activeKnowledgeBases = knowledgeBases.stream()
                .filter(KnowledgeBase::getIsActive)
                .collect(Collectors.toSet());

        if (activeKnowledgeBases.isEmpty()) {
            return RagContext.builder()
                    .originalSystemPrompt(baseSystemPrompt)
                    .augmentedSystemPrompt(baseSystemPrompt)
                    .contextFound(false)
                    .build();
        }

        // Use RAG service to build augmented context
        return ragService.buildRagContext(baseSystemPrompt, query, activeKnowledgeBases);
    }

    private String buildBaseSystemPrompt(Agent agent) {
        StringBuilder sb = new StringBuilder();

        if (agent.getSystemPrompt() != null && !agent.getSystemPrompt().isEmpty()) {
            sb.append(agent.getSystemPrompt());
        } else {
            sb.append("You are a helpful AI assistant.");
        }

        // TODO: Add function definitions if agent has functions

        return sb.toString();
    }

    private String buildUserMessage(AgentChatRequest request) {
        StringBuilder sb = new StringBuilder();

        // Add conversation history if provided
        if (request.getHistory() != null && !request.getHistory().isEmpty()) {
            for (AgentChatRequest.ChatMessage msg : request.getHistory()) {
                sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n\n");
            }
        }

        // Add current message
        sb.append(request.getMessage());

        return sb.toString();
    }
}
