package com.aiagent.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a function call execution to be sent back to the LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallResult {

    /**
     * The function call that was executed.
     */
    private FunctionCall functionCall;

    /**
     * Whether the execution was successful.
     */
    private boolean success;

    /**
     * The result of the function execution (as a string for LLM context).
     */
    private String result;

    /**
     * Error message if execution failed.
     */
    private String error;

    /**
     * Execution time in milliseconds.
     */
    private long executionTimeMs;

    public static FunctionCallResult success(FunctionCall call, String result, long executionTimeMs) {
        return FunctionCallResult.builder()
                .functionCall(call)
                .success(true)
                .result(result)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    public static FunctionCallResult failure(FunctionCall call, String error, long executionTimeMs) {
        return FunctionCallResult.builder()
                .functionCall(call)
                .success(false)
                .error(error)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * Get the content to send back to the LLM as a tool response.
     */
    public String getContentForLlm() {
        if (success) {
            return result != null ? result : "Function executed successfully";
        } else {
            return "Error: " + (error != null ? error : "Unknown error");
        }
    }
}
