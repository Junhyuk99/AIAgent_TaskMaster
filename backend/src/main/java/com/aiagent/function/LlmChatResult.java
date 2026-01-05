package com.aiagent.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * Result of an LLM chat that may contain function calls.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmChatResult {

    /**
     * The text content of the response (may be null if function calls present or streaming).
     */
    private String content;

    /**
     * Streaming content (used for streaming responses).
     */
    private Flux<String> contentStream;

    /**
     * List of function calls requested by the LLM.
     */
    private List<FunctionCall> functionCalls;

    /**
     * Whether the response is complete or requires function execution.
     */
    private boolean requiresFunctionExecution;

    /**
     * Whether this is a streaming response.
     */
    private boolean streaming;

    /**
     * The finish reason from the LLM (e.g., "stop", "tool_calls").
     */
    private String finishReason;

    /**
     * Check if this result has any function calls.
     */
    public boolean hasFunctionCalls() {
        return functionCalls != null && !functionCalls.isEmpty();
    }

    /**
     * Check if this is a streaming response.
     */
    public boolean isStreaming() {
        return streaming && contentStream != null;
    }

    /**
     * Create a simple text response result.
     */
    public static LlmChatResult textResponse(String content) {
        return LlmChatResult.builder()
                .content(content)
                .requiresFunctionExecution(false)
                .streaming(false)
                .finishReason("stop")
                .build();
    }

    /**
     * Create a streaming text response result.
     */
    public static LlmChatResult streamingResponse(Flux<String> contentStream) {
        return LlmChatResult.builder()
                .contentStream(contentStream)
                .requiresFunctionExecution(false)
                .streaming(true)
                .finishReason("stop")
                .build();
    }

    /**
     * Create a function call response result.
     */
    public static LlmChatResult functionCallResponse(List<FunctionCall> calls) {
        return LlmChatResult.builder()
                .functionCalls(calls)
                .requiresFunctionExecution(true)
                .streaming(false)
                .finishReason("tool_calls")
                .build();
    }
}
