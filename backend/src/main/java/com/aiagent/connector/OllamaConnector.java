package com.aiagent.connector;

import com.aiagent.dto.llm.ConnectionTestResponse;
import com.aiagent.dto.llm.ModelInfo;
import com.aiagent.function.FunctionCall;
import com.aiagent.function.LlmChatResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
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

@Slf4j
public class OllamaConnector implements LlmConnector {

    private final WebClient webClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaConnector(String baseUrl, String apiKey) {
        this.baseUrl = baseUrl;
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
                    .uri("/api/tags")
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
            OllamaTagsResponse response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(OllamaTagsResponse.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null || response.getModels() == null) {
                return List.of();
            }

            return response.getModels().stream()
                    .map(model -> ModelInfo.builder()
                            .name(model.getName())
                            .displayName(model.getName())
                            .size(model.getSize())
                            .modifiedAt(model.getModifiedAt())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.error("Failed to get models: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public String chat(String model, String systemPrompt, String userMessage, Double temperature, Integer maxTokens) {
        try {
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(model);
            request.setStream(false);

            OllamaChatMessage systemMsg = new OllamaChatMessage();
            systemMsg.setRole("system");
            systemMsg.setContent(systemPrompt != null ? systemPrompt : "");

            OllamaChatMessage userMsg = new OllamaChatMessage();
            userMsg.setRole("user");
            userMsg.setContent(userMessage);

            request.setMessages(List.of(systemMsg, userMsg));
            request.setOptions(Map.of(
                    "temperature", temperature != null ? temperature : 0.7,
                    "num_predict", maxTokens != null ? maxTokens : 2048
            ));

            OllamaChatResponse response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            if (response != null && response.getMessage() != null) {
                return response.getMessage().getContent();
            }
            return "";
        } catch (Exception e) {
            log.error("Chat failed: {}", e.getMessage());
            throw new RuntimeException("Chat request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> chatStream(String model, String systemPrompt, String userMessage, Double temperature, Integer maxTokens) {
        OllamaChatRequest request = new OllamaChatRequest();
        request.setModel(model);
        request.setStream(true);

        OllamaChatMessage systemMsg = new OllamaChatMessage();
        systemMsg.setRole("system");
        systemMsg.setContent(systemPrompt != null ? systemPrompt : "");

        OllamaChatMessage userMsg = new OllamaChatMessage();
        userMsg.setRole("user");
        userMsg.setContent(userMessage);

        request.setMessages(List.of(systemMsg, userMsg));
        request.setOptions(Map.of(
                "temperature", temperature != null ? temperature : 0.7,
                "num_predict", maxTokens != null ? maxTokens : 2048
        ));

        return webClient.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .map(this::extractContentFromStreamResponse)
                .filter(content -> !content.isEmpty())
                .onErrorResume(e -> {
                    log.error("Chat stream failed: {}", e.getMessage());
                    return Flux.error(new RuntimeException("Chat stream failed: " + e.getMessage(), e));
                });
    }

    @Override
    public String generate(String model, String prompt, Double temperature, Integer maxTokens) {
        try {
            OllamaGenerateRequest request = new OllamaGenerateRequest();
            request.setModel(model);
            request.setPrompt(prompt);
            request.setStream(false);
            request.setOptions(Map.of(
                    "temperature", temperature != null ? temperature : 0.7,
                    "num_predict", maxTokens != null ? maxTokens : 2048
            ));

            OllamaGenerateResponse response = webClient.post()
                    .uri("/api/generate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaGenerateResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            if (response != null) {
                return response.getResponse();
            }
            return "";
        } catch (Exception e) {
            log.error("Generate failed: {}", e.getMessage());
            throw new RuntimeException("Generate request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> generateStream(String model, String prompt, Double temperature, Integer maxTokens) {
        OllamaGenerateRequest request = new OllamaGenerateRequest();
        request.setModel(model);
        request.setPrompt(prompt);
        request.setStream(true);
        request.setOptions(Map.of(
                "temperature", temperature != null ? temperature : 0.7,
                "num_predict", maxTokens != null ? maxTokens : 2048
        ));

        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .map(this::extractResponseFromStreamResponse)
                .filter(content -> !content.isEmpty())
                .onErrorResume(e -> {
                    log.error("Generate stream failed: {}", e.getMessage());
                    return Flux.error(new RuntimeException("Generate stream failed: " + e.getMessage(), e));
                });
    }

    @Override
    public LlmChatResult chatWithTools(String model, String systemPrompt, String userMessage,
                                        Double temperature, Integer maxTokens,
                                        List<Map<String, Object>> tools) {
        try {
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(model);
            request.setStream(false);

            OllamaChatMessage systemMsg = new OllamaChatMessage();
            systemMsg.setRole("system");
            systemMsg.setContent(systemPrompt != null ? systemPrompt : "");

            OllamaChatMessage userMsg = new OllamaChatMessage();
            userMsg.setRole("user");
            userMsg.setContent(userMessage);

            request.setMessages(List.of(systemMsg, userMsg));
            request.setOptions(Map.of(
                    "temperature", temperature != null ? temperature : 0.7,
                    "num_predict", maxTokens != null ? maxTokens : 2048
            ));

            // Add tools if provided
            if (tools != null && !tools.isEmpty()) {
                request.setTools(tools);
                log.info("Sending {} tools to Ollama model {}", tools.size(), model);
                for (Map<String, Object> tool : tools) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> func = (Map<String, Object>) tool.get("function");
                    if (func != null) {
                        log.info("  Tool: {}", func.get("name"));
                    }
                }
            }

            // Log the raw request for debugging
            try {
                String requestJson = objectMapper.writeValueAsString(request);
                log.debug("Ollama request: {}", requestJson);
            } catch (Exception e) {
                log.debug("Failed to serialize request for logging");
            }

            String rawResponse = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            log.info("Ollama raw response: {}", rawResponse != null && rawResponse.length() > 500
                    ? rawResponse.substring(0, 500) + "..." : rawResponse);

            OllamaChatResponse response = objectMapper.readValue(rawResponse, OllamaChatResponse.class);

            return parseOllamaResponse(response);

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
            OllamaChatRequest request = new OllamaChatRequest();
            request.setModel(model);
            request.setStream(false);

            // Build messages list from conversation history
            List<OllamaChatMessage> messages = new ArrayList<>();

            // Add system message
            OllamaChatMessage systemMsg = new OllamaChatMessage();
            systemMsg.setRole("system");
            systemMsg.setContent(systemPrompt != null ? systemPrompt : "");
            messages.add(systemMsg);

            // Add conversation history
            for (Map<String, Object> historyMsg : conversationHistory) {
                OllamaChatMessage msg = new OllamaChatMessage();
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
                    List<OllamaToolCall> toolCalls = new ArrayList<>();
                    for (Map<String, Object> tc : toolCallsRaw) {
                        OllamaToolCall toolCall = new OllamaToolCall();
                        toolCall.setId((String) tc.get("id"));
                        toolCall.setType((String) tc.get("type"));

                        @SuppressWarnings("unchecked")
                        Map<String, Object> functionMap = (Map<String, Object>) tc.get("function");
                        if (functionMap != null) {
                            OllamaFunctionCall functionCall = new OllamaFunctionCall();
                            functionCall.setName((String) functionMap.get("name"));
                            @SuppressWarnings("unchecked")
                            Map<String, Object> args = (Map<String, Object>) functionMap.get("arguments");
                            functionCall.setArguments(args);
                            toolCall.setFunction(functionCall);
                        }
                        toolCalls.add(toolCall);
                    }
                    msg.setToolCalls(toolCalls);
                }

                messages.add(msg);
            }

            request.setMessages(messages);
            request.setOptions(Map.of(
                    "temperature", temperature != null ? temperature : 0.7,
                    "num_predict", maxTokens != null ? maxTokens : 2048
            ));

            if (tools != null && !tools.isEmpty()) {
                request.setTools(tools);
            }

            OllamaChatResponse response = webClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            return parseOllamaResponse(response);

        } catch (Exception e) {
            log.error("Continue with function result failed: {}", e.getMessage());
            throw new RuntimeException("Continue with function result failed: " + e.getMessage(), e);
        }
    }

    /**
     * Parse Ollama response into LlmChatResult.
     */
    private LlmChatResult parseOllamaResponse(OllamaChatResponse response) {
        if (response == null || response.getMessage() == null) {
            return LlmChatResult.textResponse("");
        }

        OllamaChatMessage message = response.getMessage();

        // Check for tool calls
        if (message.getToolCalls() != null && !message.getToolCalls().isEmpty()) {
            List<FunctionCall> functionCalls = new ArrayList<>();

            for (OllamaToolCall toolCall : message.getToolCalls()) {
                if (toolCall.getFunction() != null) {
                    FunctionCall fc = FunctionCall.builder()
                            .id(toolCall.getId())
                            .name(toolCall.getFunction().getName())
                            .arguments(toolCall.getFunction().getArguments())
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

    private String extractContentFromStreamResponse(String jsonLine) {
        try {
            OllamaChatResponse response = objectMapper.readValue(jsonLine, OllamaChatResponse.class);
            if (response.getMessage() != null && response.getMessage().getContent() != null) {
                return response.getMessage().getContent();
            }
        } catch (Exception e) {
            log.debug("Failed to parse stream response: {}", jsonLine);
        }
        return "";
    }

    private String extractResponseFromStreamResponse(String jsonLine) {
        try {
            OllamaGenerateResponse response = objectMapper.readValue(jsonLine, OllamaGenerateResponse.class);
            if (response.getResponse() != null) {
                return response.getResponse();
            }
        } catch (Exception e) {
            log.debug("Failed to parse stream response: {}", jsonLine);
        }
        return "";
    }

    @Data
    private static class OllamaTagsResponse {
        private List<OllamaModel> models;
    }

    @Data
    private static class OllamaModel {
        private String name;
        private Long size;
        @JsonProperty("modified_at")
        private String modifiedAt;
    }

    @Data
    private static class OllamaChatRequest {
        private String model;
        private List<OllamaChatMessage> messages;
        private boolean stream;
        private Map<String, Object> options;
        private List<Map<String, Object>> tools;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaChatMessage {
        private String role;
        private String content;
        @JsonProperty("tool_calls")
        private List<OllamaToolCall> toolCalls;
        @JsonProperty("tool_call_id")
        private String toolCallId;
    }

    @Data
    private static class OllamaToolCall {
        private String id;
        private String type;
        private OllamaFunctionCall function;
    }

    @Data
    private static class OllamaFunctionCall {
        private String name;
        private Map<String, Object> arguments;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaChatResponse {
        private OllamaChatMessage message;
        private boolean done;
        @JsonProperty("done_reason")
        private String doneReason;
    }

    @Data
    private static class OllamaGenerateRequest {
        private String model;
        private String prompt;
        private boolean stream;
        private Map<String, Object> options;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaGenerateResponse {
        private String response;
        private boolean done;
    }
}
