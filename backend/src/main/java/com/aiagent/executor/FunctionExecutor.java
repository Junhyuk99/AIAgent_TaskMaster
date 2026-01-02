package com.aiagent.executor;

import java.util.Map;

/**
 * Interface for function executors.
 * Different implementation types (HTTP_API, CODE, TEMPLATE) have their own executors.
 */
public interface FunctionExecutor {

    /**
     * Execute the function with given parameters.
     *
     * @param config The implementation configuration (JSON string)
     * @param parameters The input parameters for the function
     * @return The result of the function execution
     * @throws FunctionExecutionException if execution fails
     */
    FunctionExecutionResult execute(String config, Map<String, Object> parameters) throws FunctionExecutionException;

    /**
     * Validate the configuration before execution.
     *
     * @param config The implementation configuration to validate
     * @throws IllegalArgumentException if configuration is invalid
     */
    void validateConfig(String config) throws IllegalArgumentException;
}
