package com.aiagent.service;

import com.aiagent.connector.LlmConnector;
import com.aiagent.dto.chat.AgentChatRequest;
import com.aiagent.dto.chat.AgentChatResponse;
import com.aiagent.dto.rag.RagContext;
import com.aiagent.dto.rag.RagSearchResponse;
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

    private static final String SEARCH_KNOWLEDGE_BASE_FUNCTION = "search_knowledge_base";

    private final AgentRepository agentRepository;
    private final LlmServerService llmServerService;
    private final RagService ragService;
    private final FunctionSchemaConverter functionSchemaConverter;
    private final FunctionService functionService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    @Value("${agent.function-calling.max-iterations:5}")
    private int maxFunctionCallIterations;

    // Thread-local storage for RAG sources collected during function execution
    private final ThreadLocal<List<AgentChatResponse.DocumentSource>> ragSourcesHolder = new ThreadLocal<>();

    public AgentChatResponse chat(Long agentId, Long userId, AgentChatRequest request) {
        Agent agent = getAgentWithAccess(agentId, userId);
        validateAgentConfiguration(agent);

        LlmConnector connector = llmServerService.getConnector(agent.getLlmServer().getId(), userId);

        String userMessage = buildUserMessage(request);

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

        // Check if this is a simple greeting - skip function calling entirely
        boolean isGreeting = isSimpleGreeting(request.getMessage());

        // Get tools (functions) for the agent - only if not a greeting
        Set<Function> functions = agent.getFunctions();
        List<Map<String, Object>> tools = new ArrayList<>();
        Set<KnowledgeBase> activeKnowledgeBases = Set.of();

        if (!isGreeting) {
            tools.addAll(functionSchemaConverter.convertToTools(functions));

            // Add knowledge base search function if agent has active knowledge bases
            activeKnowledgeBases = getActiveKnowledgeBases(agent);
            if (!activeKnowledgeBases.isEmpty()) {
                tools.add(buildKnowledgeBaseSearchTool(activeKnowledgeBases));
            }
        }

        boolean hasTools = !tools.isEmpty();

        // Use system prompt with tool guidelines if tools are available
        String systemPrompt = buildSystemPromptWithToolGuidelines(agent, hasTools);

        // Initialize RAG sources holder
        ragSourcesHolder.set(new ArrayList<>());

        String response;
        List<AgentChatResponse.FunctionExecution> functionExecutions = new ArrayList<>();

        try {
            if (isGreeting) {
                // Simple greeting - no function calling
                log.info("Agent {} responding to simple greeting for user {} (skipping function calling)", agentId, userId);
                response = connector.chat(
                        agent.getModelName(),
                        buildBaseSystemPrompt(agent),
                        userMessage,
                        agent.getTemperature(),
                        agent.getMaxTokens()
                );
            } else if (hasTools) {
                // Use function calling flow - LLM decides when to search knowledge base
                response = chatWithFunctionCalling(
                        connector, agent, userId, systemPrompt, userMessage, tools, functions,
                        activeKnowledgeBases, functionExecutions
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

            // Get RAG sources collected during function execution
            List<AgentChatResponse.DocumentSource> sources = ragSourcesHolder.get();
            if (sources != null && sources.isEmpty()) {
                sources = null;
            }

            long executionTimeMs = System.currentTimeMillis() - startTime;

            // Save assistant message
            conversationService.addMessage(conversation, Message.Role.ASSISTANT, response,
                    agent.getModelName(), executionTimeMs, functionExecutions, sources);

            log.info("Agent {} chat completed for user {}, KB search called: {}, functions executed: {}",
                    agentId, userId, sources != null, functionExecutions.size());

            return AgentChatResponse.builder()
                    .conversationId(conversationId)
                    .response(response)
                    .model(agent.getModelName())
                    .agentId(agent.getId())
                    .agentName(agent.getName())
                    .sources(sources)
                    .functionExecutions(functionExecutions.isEmpty() ? null : functionExecutions)
                    .build();
        } finally {
            ragSourcesHolder.remove();
        }
    }

    /**
     * Get active knowledge bases for an agent.
     */
    private Set<KnowledgeBase> getActiveKnowledgeBases(Agent agent) {
        Set<KnowledgeBase> knowledgeBases = agent.getKnowledgeBases();
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return Set.of();
        }
        return knowledgeBases.stream()
                .filter(KnowledgeBase::getIsActive)
                .collect(Collectors.toSet());
    }

    /**
     * Build the knowledge base search tool definition.
     */
    private Map<String, Object> buildKnowledgeBaseSearchTool(Set<KnowledgeBase> knowledgeBases) {
        String kbNames = knowledgeBases.stream()
                .map(KnowledgeBase::getName)
                .collect(Collectors.joining(", "));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("type", "object");
        parameters.put("properties", Map.of(
                "query", Map.of(
                        "type", "string",
                        "description", "The search query to find relevant information from the knowledge base"
                )
        ));
        parameters.put("required", List.of("query"));

        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", SEARCH_KNOWLEDGE_BASE_FUNCTION);
        function.put("description",
                "Search the knowledge base for relevant documents. " +
                "The 'query' parameter should be SHORT KEYWORDS (2-5 words), NOT a full sentence or answer. " +
                "Examples: query='Java inheritance', query='상속 개념', query='Python list methods'. " +
                "BAD examples: query='Java에서 상속은 부모 클래스의...' (this is an answer, not a search query). " +
                "ONLY use when the user asks about specific topics that might be in the documents. " +
                "DO NOT use for: greetings, casual chat, or general knowledge questions. " +
                "Available knowledge bases: " + kbNames);
        function.put("parameters", parameters);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", function);

        return tool;
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
            Set<KnowledgeBase> knowledgeBases,
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

            // Execute function calls (including knowledge base search)
            List<FunctionCallResult> results = executeFunctionCalls(
                    result.getFunctionCalls(), functions, knowledgeBases, userId
            );

            // Add assistant message with tool calls
            conversationHistory.add(buildAssistantToolCallMessage(result.getFunctionCalls()));

            // Add tool response messages
            for (FunctionCallResult fcResult : results) {
                conversationHistory.add(buildToolResponseMessage(fcResult));

                // Record function execution for response (skip internal KB search from display)
                if (!SEARCH_KNOWLEDGE_BASE_FUNCTION.equals(fcResult.getFunctionCall().getName())) {
                    functionExecutions.add(AgentChatResponse.FunctionExecution.builder()
                            .functionName(fcResult.getFunctionCall().getName())
                            .arguments(fcResult.getFunctionCall().getArguments())
                            .result(fcResult.isSuccess() ? fcResult.getResult() : null)
                            .error(fcResult.getError())
                            .executionTimeMs(fcResult.getExecutionTimeMs())
                            .build());
                }
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
            Set<KnowledgeBase> knowledgeBases,
            Long userId) {

        List<FunctionCallResult> results = new ArrayList<>();

        for (FunctionCall call : functionCalls) {
            long startTime = System.currentTimeMillis();

            try {
                // Check if this is the built-in knowledge base search function
                if (SEARCH_KNOWLEDGE_BASE_FUNCTION.equals(call.getName())) {
                    FunctionCallResult kbResult = executeKnowledgeBaseSearch(call, knowledgeBases, startTime);
                    results.add(kbResult);
                    continue;
                }

                // Find the user-defined function by name
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
     * Execute knowledge base search function.
     */
    private FunctionCallResult executeKnowledgeBaseSearch(
            FunctionCall call,
            Set<KnowledgeBase> knowledgeBases,
            long startTime) {

        try {
            // Parse the query from arguments
            Map<String, Object> arguments = call.getArguments();
            String query = null;

            if (arguments != null) {
                Object queryObj = arguments.get("query");
                if (queryObj != null) {
                    query = queryObj.toString();
                }
            }

            if (query == null || query.trim().isEmpty()) {
                return FunctionCallResult.failure(call, "Query parameter is required",
                        System.currentTimeMillis() - startTime);
            }

            log.info("Executing knowledge base search with query: {}", query);

            // Use RAG service to search
            RagContext ragContext = ragService.buildRagContext("", query, knowledgeBases);

            if (!ragContext.isContextFound() || ragContext.getSources() == null || ragContext.getSources().isEmpty()) {
                return FunctionCallResult.success(call,
                        "No relevant information found in the knowledge base for this query.",
                        System.currentTimeMillis() - startTime);
            }

            // Store sources for response
            List<AgentChatResponse.DocumentSource> sources = ragSourcesHolder.get();
            if (sources != null) {
                for (RagSearchResponse.DocumentSource source : ragContext.getSources()) {
                    sources.add(AgentChatResponse.DocumentSource.builder()
                            .documentId(source.getDocumentId())
                            .documentName(source.getDocumentName())
                            .score(source.getScore())
                            .build());
                }
            }

            // Return the context as the function result
            String contextText = ragContext.getAugmentedSystemPrompt();
            // Extract just the context part (remove the original system prompt prefix if any)
            if (contextText.contains("Reference Information:")) {
                contextText = contextText.substring(contextText.indexOf("Reference Information:"));
            }

            return FunctionCallResult.success(call, contextText, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Knowledge base search failed: {}", e.getMessage());
            return FunctionCallResult.failure(call, "Search failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
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

        String userMessage = buildUserMessage(request);
        String systemPrompt = buildBaseSystemPrompt(agent);

        // Check if this is a simple greeting - skip function calling entirely
        if (isSimpleGreeting(request.getMessage())) {
            log.info("Agent {} streaming simple greeting response for user {} (skipping function calling)", agentId, userId);
            return connector.chatStream(
                    agent.getModelName(),
                    systemPrompt,
                    userMessage,
                    agent.getTemperature(),
                    agent.getMaxTokens()
            );
        }

        // Get tools (functions) for the agent
        Set<Function> functions = agent.getFunctions();
        List<Map<String, Object>> tools = new ArrayList<>(functionSchemaConverter.convertToTools(functions));

        // Add knowledge base search function if agent has active knowledge bases
        Set<KnowledgeBase> activeKnowledgeBases = getActiveKnowledgeBases(agent);
        if (!activeKnowledgeBases.isEmpty()) {
            tools.add(buildKnowledgeBaseSearchTool(activeKnowledgeBases));
        }

        boolean hasTools = !tools.isEmpty();

        // Use system prompt with tool guidelines if tools are available
        systemPrompt = buildSystemPromptWithToolGuidelines(agent, hasTools);

        if (!hasTools) {
            // No tools, just stream normally
            log.info("Agent {} streaming chat started for user {} (no tools)", agentId, userId);
            return connector.chatStream(
                    agent.getModelName(),
                    systemPrompt,
                    userMessage,
                    agent.getTemperature(),
                    agent.getMaxTokens()
            );
        }

        log.info("Agent {} streaming chat with function calling started for user {}", agentId, userId);

        // Use function calling with streaming
        return chatStreamWithFunctionCalling(
                connector, agent, userId, systemPrompt, userMessage, tools, functions, activeKnowledgeBases
        );
    }

    /**
     * Check if the message is a simple greeting that doesn't need function calling.
     */
    private boolean isSimpleGreeting(String message) {
        if (message == null || message.trim().isEmpty()) {
            log.info("isSimpleGreeting: message is null or empty");
            return false;
        }

        String normalized = message.trim().toLowerCase();
        log.info("isSimpleGreeting: checking message '{}', normalized '{}', length={}", message, normalized, normalized.length());

        // Common greetings in various languages
        List<String> greetings = List.of(
                // Korean
                "안녕", "안녕하세요", "안녕하십니까", "반갑습니다", "반가워요", "반가워",
                "하이", "헬로", "좋은 아침", "좋은 저녁", "좋은 밤",
                // English
                "hi", "hello", "hey", "good morning", "good afternoon", "good evening",
                "good night", "howdy", "greetings", "yo", "sup", "what's up", "whats up",
                // Japanese
                "こんにちは", "こんばんは", "おはよう",
                // Chinese
                "你好", "您好"
        );

        // Check exact match
        for (String greeting : greetings) {
            if (normalized.equals(greeting)) {
                log.info("isSimpleGreeting: exact match found for '{}'", greeting);
                return true;
            }
        }

        // Check if starts with greeting and is short (less than 20 chars)
        if (normalized.length() < 20) {
            for (String greeting : greetings) {
                if (normalized.startsWith(greeting)) {
                    log.info("isSimpleGreeting: prefix match found for '{}' in message '{}'", greeting, normalized);
                    return true;
                }
            }
        }

        log.info("isSimpleGreeting: no greeting match found for '{}'", normalized);
        return false;
    }

    /**
     * Handle streaming chat with function calling.
     * Sends SSE events for function calls and results, then streams the final response.
     */
    private Flux<String> chatStreamWithFunctionCalling(
            LlmConnector connector,
            Agent agent,
            Long userId,
            String systemPrompt,
            String userMessage,
            List<Map<String, Object>> tools,
            Set<Function> functions,
            Set<KnowledgeBase> knowledgeBases) {

        return Flux.create(sink -> {
            try {
                // Initialize RAG sources holder for this request
                List<AgentChatResponse.DocumentSource> ragSources = new ArrayList<>();
                List<AgentChatResponse.FunctionExecution> functionExecutions = new ArrayList<>();

                // Initial chat with tools
                LlmChatResult result = connector.chatWithToolsStreaming(
                        agent.getModelName(),
                        systemPrompt,
                        userMessage,
                        agent.getTemperature(),
                        agent.getMaxTokens(),
                        tools
                );

                // Build conversation history for function calling loop
                List<Map<String, Object>> conversationHistory = new ArrayList<>();
                conversationHistory.add(Map.of("role", "user", "content", userMessage));

                int iterations = 0;
                while (result.hasFunctionCalls() && iterations < maxFunctionCallIterations) {
                    iterations++;

                    // Send function call events (including KB search for status display)
                    for (FunctionCall call : result.getFunctionCalls()) {
                        String functionCallEvent = String.format(
                                "{\"type\":\"function_call\",\"name\":\"%s\",\"arguments\":%s}",
                                call.getName(),
                                serializeArguments(call.getArguments())
                        );
                        sink.next(functionCallEvent);
                    }

                    // Execute function calls
                    long startTime = System.currentTimeMillis();
                    List<FunctionCallResult> results = executeFunctionCallsWithSources(
                            result.getFunctionCalls(), functions, knowledgeBases, userId, ragSources
                    );

                    // Add assistant message with tool calls to history
                    conversationHistory.add(buildAssistantToolCallMessage(result.getFunctionCalls()));

                    // Add tool response messages and send result events
                    for (FunctionCallResult fcResult : results) {
                        conversationHistory.add(buildToolResponseMessage(fcResult));

                        // Send function result event (including KB search for status display)
                        String functionResultEvent = String.format(
                                "{\"type\":\"function_result\",\"name\":\"%s\",\"success\":%b,\"executionTimeMs\":%d}",
                                fcResult.getFunctionCall().getName(),
                                fcResult.isSuccess(),
                                fcResult.getExecutionTimeMs()
                        );
                        sink.next(functionResultEvent);

                        // Record for response (skip internal KB search from final display)
                        if (!SEARCH_KNOWLEDGE_BASE_FUNCTION.equals(fcResult.getFunctionCall().getName())) {
                            functionExecutions.add(AgentChatResponse.FunctionExecution.builder()
                                    .functionName(fcResult.getFunctionCall().getName())
                                    .arguments(fcResult.getFunctionCall().getArguments())
                                    .result(fcResult.isSuccess() ? fcResult.getResult() : null)
                                    .error(fcResult.getError())
                                    .executionTimeMs(fcResult.getExecutionTimeMs())
                                    .build());
                        }
                    }

                    // Continue conversation with function results
                    log.info("Continuing conversation with {} function results", results.size());
                    result = connector.continueWithFunctionResultStreaming(
                            agent.getModelName(),
                            systemPrompt,
                            conversationHistory,
                            agent.getTemperature(),
                            agent.getMaxTokens(),
                            tools
                    );
                    log.info("Continuation result - hasFunctionCalls: {}, isStreaming: {}, hasContent: {}",
                            result.hasFunctionCalls(), result.isStreaming(), result.getContent() != null);
                }

                // Send sources event if RAG was used
                if (!ragSources.isEmpty()) {
                    try {
                        String sourcesJson = objectMapper.writeValueAsString(ragSources);
                        sink.next("{\"type\":\"sources\",\"data\":" + sourcesJson + "}");
                    } catch (Exception e) {
                        log.warn("Failed to serialize sources: {}", e.getMessage());
                    }
                }

                // Send function executions summary if any functions were called
                if (!functionExecutions.isEmpty()) {
                    try {
                        String executionsJson = objectMapper.writeValueAsString(functionExecutions);
                        sink.next("{\"type\":\"function_executions\",\"data\":" + executionsJson + "}");
                    } catch (Exception e) {
                        log.warn("Failed to serialize function executions: {}", e.getMessage());
                    }
                }

                // Now stream the final response
                log.info("Final result state - isStreaming: {}, hasContentStream: {}, hasContent: {}, contentLength: {}",
                        result.isStreaming(),
                        result.getContentStream() != null,
                        result.getContent() != null,
                        result.getContent() != null ? result.getContent().length() : 0);

                if (result.isStreaming() && result.getContentStream() != null) {
                    log.info("Streaming final response from contentStream");
                    result.getContentStream()
                            .doOnNext(chunk -> sink.next("{\"type\":\"text\",\"content\":\"" + escapeJson(chunk) + "\"}"))
                            .doOnComplete(() -> {
                                sink.next("{\"type\":\"done\"}");
                                sink.complete();
                            })
                            .doOnError(sink::error)
                            .subscribe();
                } else if (result.getContent() != null && !result.getContent().isEmpty()) {
                    // Non-streaming response with content
                    log.info("Sending non-streaming response content: {}",
                            result.getContent().length() > 100 ? result.getContent().substring(0, 100) + "..." : result.getContent());
                    sink.next("{\"type\":\"text\",\"content\":\"" + escapeJson(result.getContent()) + "\"}");
                    sink.next("{\"type\":\"done\"}");
                    sink.complete();
                } else {
                    log.warn("No content to stream - result may be empty");
                    sink.next("{\"type\":\"done\"}");
                    sink.complete();
                }

            } catch (Exception e) {
                log.error("Streaming chat with function calling failed: {}", e.getMessage());
                sink.error(e);
            }
        });
    }

    /**
     * Execute function calls and collect RAG sources.
     */
    private List<FunctionCallResult> executeFunctionCallsWithSources(
            List<FunctionCall> functionCalls,
            Set<Function> functions,
            Set<KnowledgeBase> knowledgeBases,
            Long userId,
            List<AgentChatResponse.DocumentSource> ragSources) {

        List<FunctionCallResult> results = new ArrayList<>();

        for (FunctionCall call : functionCalls) {
            long startTime = System.currentTimeMillis();

            try {
                // Check if this is the built-in knowledge base search function
                if (SEARCH_KNOWLEDGE_BASE_FUNCTION.equals(call.getName())) {
                    FunctionCallResult kbResult = executeKnowledgeBaseSearchWithSources(
                            call, knowledgeBases, startTime, ragSources);
                    results.add(kbResult);
                    continue;
                }

                // Find the user-defined function by name
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
     * Execute knowledge base search and collect sources.
     */
    private FunctionCallResult executeKnowledgeBaseSearchWithSources(
            FunctionCall call,
            Set<KnowledgeBase> knowledgeBases,
            long startTime,
            List<AgentChatResponse.DocumentSource> ragSources) {

        try {
            Map<String, Object> arguments = call.getArguments();
            String query = null;

            if (arguments != null) {
                Object queryObj = arguments.get("query");
                if (queryObj != null) {
                    query = queryObj.toString();
                }
            }

            if (query == null || query.trim().isEmpty()) {
                return FunctionCallResult.failure(call, "Query parameter is required",
                        System.currentTimeMillis() - startTime);
            }

            log.info("Executing knowledge base search with query: {}", query);

            RagContext ragContext = ragService.buildRagContext("", query, knowledgeBases);

            if (!ragContext.isContextFound() || ragContext.getSources() == null || ragContext.getSources().isEmpty()) {
                return FunctionCallResult.success(call,
                        "No relevant information found in the knowledge base for this query.",
                        System.currentTimeMillis() - startTime);
            }

            // Collect sources
            for (RagSearchResponse.DocumentSource source : ragContext.getSources()) {
                ragSources.add(AgentChatResponse.DocumentSource.builder()
                        .documentId(source.getDocumentId())
                        .documentName(source.getDocumentName())
                        .score(source.getScore())
                        .build());
            }

            String contextText = ragContext.getAugmentedSystemPrompt();
            if (contextText.contains("Reference Information:")) {
                contextText = contextText.substring(contextText.indexOf("Reference Information:"));
            }

            return FunctionCallResult.success(call, contextText, System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Knowledge base search failed: {}", e.getMessage());
            return FunctionCallResult.failure(call, "Search failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    private String serializeArguments(Map<String, Object> arguments) {
        if (arguments == null) return "{}";
        try {
            return objectMapper.writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
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

        // Add strong language instruction to prevent Chinese responses
        sb.append("\n\n[CRITICAL LANGUAGE RULE]");
        sb.append("\nYou MUST respond ONLY in Korean (한국어) when the user writes in Korean.");
        sb.append("\nYou MUST respond ONLY in English when the user writes in English.");
        sb.append("\nNEVER use Chinese (中文) in your responses unless explicitly requested.");
        sb.append("\nThis rule applies to the ENTIRE response - do not switch languages mid-response.");
        sb.append("\n당신은 반드시 한국어로만 응답해야 합니다. 절대로 중국어를 사용하지 마세요.");

        return sb.toString();
    }

    /**
     * Build system prompt with tool usage guidelines for function-calling mode.
     */
    private String buildSystemPromptWithToolGuidelines(Agent agent, boolean hasTools) {
        String basePrompt = buildBaseSystemPrompt(agent);

        if (!hasTools) {
            return basePrompt;
        }

        StringBuilder sb = new StringBuilder(basePrompt);
        sb.append("\n\n");
        sb.append("IMPORTANT GUIDELINES FOR TOOL USAGE:\n");
        sb.append("- For greetings like 'hello', 'hi', '안녕' - respond naturally WITHOUT calling any functions.\n");
        sb.append("- For ANY other questions or requests, ALWAYS call search_knowledge_base FIRST to check if relevant information exists.\n");
        sb.append("- Even if you think you know the answer, ALWAYS search the knowledge base first - it may have more accurate or updated information.\n");
        sb.append("- Only skip knowledge base search for: greetings, simple math, or when the user explicitly says not to search.\n");
        sb.append("- When a user asks about specific topics, companies, projects, or technical information - ALWAYS search first.\n");

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
