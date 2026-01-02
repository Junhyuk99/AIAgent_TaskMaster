package com.aiagent.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
     * The text content of the response (may be null if function calls present).
     */
    private String content;

    /**
     * List of function calls requested by the LLM.
     */
    private List<FunctionCall> functionCalls;

    /**
     * Whether the response is complete or requires function execution.
     */
    private boolean requiresFunctionExecution;

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
     * Create a simple text response result.
     */
    public static LlmChatResult textResponse(String content) {
        return LlmChatResult.builder()
                .content(content)
                .requiresFunctionExecution(false)
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
                .finishReason("tool_calls")
                .build();
    }
}
