package com.aiagent.connector;

import com.aiagent.dto.llm.ConnectionTestResponse;
import com.aiagent.dto.llm.ModelInfo;
import com.aiagent.function.FunctionCallResult;
import com.aiagent.function.LlmChatResult;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface LlmConnector {

    ConnectionTestResponse testConnection();

    List<ModelInfo> getModels();

    String chat(String model, String systemPrompt, String userMessage, Double temperature, Integer maxTokens);

    Flux<String> chatStream(String model, String systemPrompt, String userMessage, Double temperature, Integer maxTokens);

    String generate(String model, String prompt, Double temperature, Integer maxTokens);

    Flux<String> generateStream(String model, String prompt, Double temperature, Integer maxTokens);

    /**
     * Chat with function calling support.
     *
     * @param model The model to use
     * @param systemPrompt System prompt
     * @param userMessage User message
     * @param temperature Temperature setting
     * @param maxTokens Maximum tokens
     * @param tools List of tool definitions in OpenAI format
     * @return LlmChatResult containing either text or function calls
     */
    default LlmChatResult chatWithTools(String model, String systemPrompt, String userMessage,
                                         Double temperature, Integer maxTokens,
                                         List<Map<String, Object>> tools) {
        // Default implementation falls back to regular chat (no function calling)
        String response = chat(model, systemPrompt, userMessage, temperature, maxTokens);
        return LlmChatResult.textResponse(response);
    }

    /**
     * Continue chat after function execution.
     *
     * @param model The model to use
     * @param systemPrompt System prompt
     * @param conversationHistory Full conversation history including tool responses
     * @param temperature Temperature setting
     * @param maxTokens Maximum tokens
     * @param tools List of tool definitions
     * @return LlmChatResult containing either text or more function calls
     */
    default LlmChatResult continueWithFunctionResult(String model, String systemPrompt,
                                                      List<Map<String, Object>> conversationHistory,
                                                      Double temperature, Integer maxTokens,
                                                      List<Map<String, Object>> tools) {
        // Default implementation - connectors should override this
        throw new UnsupportedOperationException("Function calling not supported by this connector");
    }

    /**
     * Chat with tools support, returning streaming LlmChatResult.
     * This method first makes a non-streaming call to check for function calls,
     * then streams the final response.
     *
     * @param model The model to use
     * @param systemPrompt System prompt
     * @param userMessage User message
     * @param temperature Temperature setting
     * @param maxTokens Maximum tokens
     * @param tools List of tool definitions
     * @return LlmChatResult containing function calls or a Flux for text streaming
     */
    default LlmChatResult chatWithToolsStreaming(String model, String systemPrompt, String userMessage,
                                                  Double temperature, Integer maxTokens,
                                                  List<Map<String, Object>> tools) {
        // Default implementation falls back to non-streaming
        return chatWithTools(model, systemPrompt, userMessage, temperature, maxTokens, tools);
    }

    /**
     * Continue chat after function execution with streaming response.
     *
     * @param model The model to use
     * @param systemPrompt System prompt
     * @param conversationHistory Full conversation history including tool responses
     * @param temperature Temperature setting
     * @param maxTokens Maximum tokens
     * @param tools List of tool definitions
     * @return LlmChatResult with streaming text or more function calls
     */
    default LlmChatResult continueWithFunctionResultStreaming(String model, String systemPrompt,
                                                               List<Map<String, Object>> conversationHistory,
                                                               Double temperature, Integer maxTokens,
                                                               List<Map<String, Object>> tools) {
        // Default implementation falls back to non-streaming
        return continueWithFunctionResult(model, systemPrompt, conversationHistory, temperature, maxTokens, tools);
    }
}
