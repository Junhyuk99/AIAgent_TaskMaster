package com.aiagent.executor;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * Result of a function execution.
 */
@Data
@Builder
public class FunctionExecutionResult {

    private boolean success;
    private Object data;
    private String error;
    private long executionTimeMs;
    private Map<String, Object> metadata;

    public static FunctionExecutionResult success(Object data, long executionTimeMs) {
        return FunctionExecutionResult.builder()
                .success(true)
                .data(data)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    public static FunctionExecutionResult success(Object data, long executionTimeMs, Map<String, Object> metadata) {
        return FunctionExecutionResult.builder()
                .success(true)
                .data(data)
                .executionTimeMs(executionTimeMs)
                .metadata(metadata)
                .build();
    }

    public static FunctionExecutionResult failure(String error, long executionTimeMs) {
        return FunctionExecutionResult.builder()
                .success(false)
                .error(error)
                .executionTimeMs(executionTimeMs)
                .build();
    }
}
