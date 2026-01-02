package com.aiagent.connector;

import com.aiagent.dto.llm.ConnectionTestResponse;
import com.aiagent.dto.llm.ModelInfo;
import com.aiagent.function.FunctionCall;
import com.aiagent.function.LlmChatResult;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connector for OpenAI-compatible APIs (vLLM, LMStudio, LocalAI, etc.)
 * Uses the standard OpenAI API format:
 * - GET /v1/models
 * - POST /v1/chat/completions
 */
@Slf4j
public class OpenAICompatibleConnector implements LlmConnector {

    private final WebClient webClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public OpenAICompatibleConnector(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        WebClient.Builder builder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            builder.defaultHeader("Authorization", "Bearer " + apiKey);
        }

        this.webClient = builder.build();
    }

    @Override
    public ConnectionTestResponse testConnection() {
        long startTime = System.currentTimeMillis();
        try {
            webClient.get()
                    .uri("/v1/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            return ConnectionTestResponse.builder()
                    .success(true)
                    .message("Connection successful")
                    .responseTimeMs(responseTime)
                    .build();
        } catch (WebClientResponseException e) {
            log.error("Connection test failed: {}", e.getMessage());
            return ConnectionTestResponse.builder()
                    .success(false)
                    .message("HTTP Error: " + e.getStatusCode())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        } catch (Exception e) {
            log.error("Connection test failed: {}", e.getMessage());
            return ConnectionTestResponse.builder()
                    .success(false)
                    .message("Connection failed: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public List<ModelInfo> getModels() {
        try {
            String response = webClient.get()
                    .uri("/v1/models")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) {
                return List.of();
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.get("data");

            if (dataNode == null || !dataNode.isArray()) {
                return List.of();
            }

            List<ModelInfo> models = new ArrayList<>();
            for (JsonNode modelNode : dataNode) {
                String id = modelNode.has("id") ? modelNode.get("id").asText() : null;
                if (id != null) {
                    ModelInfo info = ModelInfo.builder()
                            .name(id)
                            .displayName(id)
                            .build();

                    // Parse optional fields if available
                    if (modelNode.has("created")) {
                        info.setModifiedAt(String.valueOf(modelNode.get("created").asLong()));
                    }

                    models.add(info);
                }
            }

            return models;
        } catch (Exception e) {
            log.error("Failed to get models: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String chat(String model, String systemPrompt, String userMessage, Double temperature, Integer maxTokens) {
        try {
            ChatCompletionRequest request = buildChatRequest(model, systemPrompt, userMessage, temperature, maxTokens, false, null);

            ChatCompletionResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            if (response != null && response.getChoices() != null && !response.getChoices().isEmpty()) {
                ChatChoice choice = response.getChoices().get(0);
                if (choice.getMessage() != null) {
                    return choice.getMessage().getContent() != null ? choice.getMessage().getContent() : "";
                }
            }
            return "";
        } catch (Exception e) {
            log.error("Chat failed: {}", e.getMessage());
            throw new RuntimeException("Chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(String model, String systemPrompt, String userMessage, Double temperature, Integer maxTokens) {
        ChatCompletionRequest request = buildChatRequest(model, systemPrompt, userMessage, temperature, maxTokens, true, null);

        return webClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .mapNotNull(this::extractStreamContent)
                .filter(content -> !content.isEmpty())
                .onErrorResume(e -> {
                    log.error("Chat stream failed: {}", e.getMessage());
                    return Flux.error(new RuntimeException("Chat stream failed: " + e.getMessage(), e));
                });
    }

    @Override
    public String generate(String model, String prompt, Double temperature, Integer maxTokens) {
        // OpenAI-compatible API uses chat completions, so convert prompt to a user message
        return chat(model, null, prompt, temperature, maxTokens);
    }

    @Override
    public Flux<String> generateStream(String model, String prompt, Double temperature, Integer maxTokens) {
        return chatStream(model, null, prompt, temperature, maxTokens);
    }

    @Override
    public LlmChatResult chatWithTools(String model, String systemPrompt, String userMessage,
                                        Double temperature, Integer maxTokens,
                                        List<Map<String, Object>> tools) {
        try {
            ChatCompletionRequest request = buildChatRequest(model, systemPrompt, userMessage, temperature, maxTokens, false, tools);

            ChatCompletionResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            return parseChatResponse(response);

        } catch (Exception e) {
            log.error("Chat with tools failed: {}", e.getMessage());
            throw new RuntimeException("Chat with tools failed: " + e.getMessage(), e);
        }
    }

    @Override
    public LlmChatResult continueWithFunctionResult(String model, String systemPrompt,
                                                     List<Map<String, Object>> conversationHistory,
                                                     Double temperature, Integer maxTokens,
                                                     List<Map<String, Object>> tools) {
        try {
            ChatCompletionRequest request = new ChatCompletionRequest();
            request.setModel(model);
            request.setStream(false);
            request.setTemperature(temperature != null ? temperature : 0.7);
            request.setMaxTokens(maxTokens != null ? maxTokens : 2048);

            // Build messages list
            List<ChatMessage> messages = new ArrayList<>();

            // Add system message
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                ChatMessage systemMsg = new ChatMessage();
                systemMsg.setRole("system");
                systemMsg.setContent(systemPrompt);
                messages.add(systemMsg);
            }

            // Add conversation history
            for (Map<String, Object> historyMsg : conversationHistory) {
                ChatMessage msg = new ChatMessage();
                msg.setRole((String) historyMsg.get("role"));
                msg.setContent((String) historyMsg.get("content"));

                // Handle tool call ID for tool responses
                if (historyMsg.containsKey("tool_call_id")) {
                    msg.setToolCallId((String) historyMsg.get("tool_call_id"));
                }

                // Handle tool calls for assistant messages
                if (historyMsg.containsKey("tool_calls")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> toolCallsRaw = (List<Map<String, Object>>) historyMsg.get("tool_calls");
                    List<ToolCall> toolCalls = new ArrayList<>();
                    for (Map<String, Object> tc : toolCallsRaw) {
                        ToolCall toolCall = new ToolCall();
                        toolCall.setId((String) tc.get("id"));
                        toolCall.setType((String) tc.get("type"));

                        @SuppressWarnings("unchecked")
                        Map<String, Object> functionMap = (Map<String, Object>) tc.get("function");
                        if (functionMap != null) {
                            ToolCallFunction function = new ToolCallFunction();
                            function.setName((String) functionMap.get("name"));
                            Object args = functionMap.get("arguments");
                            if (args instanceof String) {
                                function.setArguments((String) args);
                            } else if (args != null) {
                                function.setArguments(objectMapper.writeValueAsString(args));
                            }
                            toolCall.setFunction(function);
                        }
                        toolCalls.add(toolCall);
                    }
                    msg.setToolCalls(toolCalls);
                }

                messages.add(msg);
            }

            request.setMessages(messages);

            if (tools != null && !tools.isEmpty()) {
                request.setTools(tools);
            }

            ChatCompletionResponse response = webClient.post()
                    .uri("/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChatCompletionResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            return parseChatResponse(response);

        } catch (Exception e) {
            log.error("Continue with function result failed: {}", e.getMessage());
            throw new RuntimeException("Continue with function result failed: " + e.getMessage(), e);
        }
    }

    private ChatCompletionRequest buildChatRequest(String model, String systemPrompt, String userMessage,
                                                    Double temperature, Integer maxTokens, boolean stream,
                                                    List<Map<String, Object>> tools) {
        ChatCompletionRequest request = new ChatCompletionRequest();
        request.setModel(model);
        request.setStream(stream);
        request.setTemperature(temperature != null ? temperature : 0.7);
        request.setMaxTokens(maxTokens != null ? maxTokens : 2048);

        List<ChatMessage> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ChatMessage systemMsg = new ChatMessage();
            systemMsg.setRole("system");
            systemMsg.setContent(systemPrompt);
            messages.add(systemMsg);
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(userMessage);
        messages.add(userMsg);

        request.setMessages(messages);

        if (tools != null && !tools.isEmpty()) {
            request.setTools(tools);
        }

        return request;
    }

    private LlmChatResult parseChatResponse(ChatCompletionResponse response) {
        if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
            return LlmChatResult.textResponse("");
        }

        ChatChoice choice = response.getChoices().get(0);
        ChatMessage message = choice.getMessage();

        if (message == null) {
            return LlmChatResult.textResponse("");
        }

        // Check for tool calls
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            List<FunctionCall> functionCalls = new ArrayList<>();

            for (ToolCall toolCall : message.getToolCalls()) {
                if (toolCall.getFunction() != null) {
                    Map<String, Object> arguments = parseArguments(toolCall.getFunction().getArguments());

                    FunctionCall fc = FunctionCall.builder()
                            .id(toolCall.getId())
                            .name(toolCall.getFunction().getName())
                            .arguments(arguments)
                            .build();
                    functionCalls.add(fc);
                }
            }

            if (!functionCalls.isEmpty()) {
                log.info("LLM requested {} function call(s)", functionCalls.size());
                return LlmChatResult.functionCallResponse(functionCalls);
            }
        }

        // Regular text response
        return LlmChatResult.textResponse(message.getContent() != null ? message.getContent() : "");
    }

    private Map<String, Object> parseArguments(String argumentsJson) {
        if (argumentsJson == null || argumentsJson.isEmpty()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(argumentsJson, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse function arguments: {}", argumentsJson);
            return Map.of();
        }
    }

    private String extractStreamContent(String line) {
        if (line == null || line.isEmpty()) {
            return "";
        }

        // Handle SSE format: data: {...}
        String jsonData = line;
        if (line.startsWith("data: ")) {
            jsonData = line.substring(6);
        }

        // Skip [DONE] marker
        if ("[DONE]".equals(jsonData.trim())) {
            return "";
        }

        try {
            JsonNode root = objectMapper.readTree(jsonData);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode delta = choices.get(0).get("delta");
                if (delta != null && delta.has("content")) {
                    String content = delta.get("content").asText();
                    return content != null ? content : "";
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse stream response: {}", line);
        }
        return "";
    }

    // DTO classes for OpenAI-compatible API

    @Data
    private static class ChatCompletionRequest {
        private String model;
        private List<ChatMessage> messages;
        private boolean stream;
        private Double temperature;
        @JsonProperty("max_tokens")
        private Integer maxTokens;
        private List<Map<String, Object>> tools;
        @JsonProperty("tool_choice")
        private String toolChoice;
    }

    @Data
    private static class ChatMessage {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<ToolCall> toolCalls;
        @JsonProperty("tool_call_id")
        private String toolCallId;
    }

    @Data
    private static class ToolCall {
        private String id;
        private String type;
        private ToolCallFunction function;
    }

    @Data
    private static class ToolCallFunction {
        private String name;
        private String arguments;
    }

    @Data
    private static class ChatCompletionResponse {
        private String id;
        private String object;
        private Long created;
        private String model;
        private List<ChatChoice> choices;
        private Usage usage;
    }

    @Data
    private static class ChatChoice {
        private Integer index;
        private ChatMessage message;
        private ChatMessage delta;
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Data
    private static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
