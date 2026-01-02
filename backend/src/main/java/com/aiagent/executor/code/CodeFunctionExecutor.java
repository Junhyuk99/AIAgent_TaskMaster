package com.aiagent.executor.code;

import com.aiagent.executor.FunctionExecutionException;
import com.aiagent.executor.FunctionExecutionResult;
import com.aiagent.executor.FunctionExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Executor for code-based functions.
 * Currently provides a placeholder implementation.
 * Full implementation would require a sandboxed code execution environment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CodeFunctionExecutor implements FunctionExecutor {

    private final ObjectMapper objectMapper;

    @Override
    public FunctionExecutionResult execute(String configJson, Map<String, Object> parameters) throws FunctionExecutionException {
        long startTime = System.currentTimeMillis();

        try {
            CodeConfig config = parseConfig(configJson);
            validateConfig(config);

            // TODO: Implement sandboxed code execution
            // For now, return a placeholder message
            log.warn("Code execution is not yet implemented. Config: {}", config.getLanguage());

            Object result = Map.of(
                "message", "Code execution is not yet implemented",
                "language", config.getLanguage(),
                "codePreview", truncateCode(config.getCode(), 100)
            );

            long executionTime = System.currentTimeMillis() - startTime;
            return FunctionExecutionResult.success(result, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Code execution failed: {}", e.getMessage(), e);
            return FunctionExecutionResult.failure(e.getMessage(), executionTime);
        }
    }

    @Override
    public void validateConfig(String configJson) throws IllegalArgumentException {
        try {
            CodeConfig config = parseConfig(configJson);
            validateConfig(config);
        } catch (FunctionExecutionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private CodeConfig parseConfig(String configJson) throws FunctionExecutionException {
        try {
            return objectMapper.readValue(configJson, CodeConfig.class);
        } catch (JsonProcessingException e) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Failed to parse configuration: " + e.getMessage(), e);
        }
    }

    private void validateConfig(CodeConfig config) throws FunctionExecutionException {
        if (config.getCode() == null || config.getCode().isBlank()) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Code is required");
        }
    }

    private String truncateCode(String code, int maxLength) {
        if (code == null) return "";
        if (code.length() <= maxLength) return code;
        return code.substring(0, maxLength) + "...";
    }
}
