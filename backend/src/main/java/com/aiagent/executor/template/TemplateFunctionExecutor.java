package com.aiagent.executor.template;

import com.aiagent.executor.FunctionExecutionException;
import com.aiagent.executor.FunctionExecutionResult;
import com.aiagent.executor.FunctionExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executor for template-based functions.
 * Performs simple string template substitution with parameters.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateFunctionExecutor implements FunctionExecutor {

    private final ObjectMapper objectMapper;

    // Pattern for template substitution: {{paramName}}
    private static final Pattern TEMPLATE_PARAM_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    @Override
    public FunctionExecutionResult execute(String configJson, Map<String, Object> parameters) throws FunctionExecutionException {
        long startTime = System.currentTimeMillis();

        try {
            TemplateConfig config = parseConfig(configJson);
            validateConfig(config);

            String result = processTemplate(config.getTemplate(), parameters);

            // Apply output format transformation
            Object output = formatOutput(result, config);

            long executionTime = System.currentTimeMillis() - startTime;
            return FunctionExecutionResult.success(output, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Template execution failed: {}", e.getMessage(), e);
            return FunctionExecutionResult.failure(e.getMessage(), executionTime);
        }
    }

    @Override
    public void validateConfig(String configJson) throws IllegalArgumentException {
        try {
            TemplateConfig config = parseConfig(configJson);
            validateConfig(config);
        } catch (FunctionExecutionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private TemplateConfig parseConfig(String configJson) throws FunctionExecutionException {
        try {
            return objectMapper.readValue(configJson, TemplateConfig.class);
        } catch (JsonProcessingException e) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Failed to parse configuration: " + e.getMessage(), e);
        }
    }

    private void validateConfig(TemplateConfig config) throws FunctionExecutionException {
        if (config.getTemplate() == null || config.getTemplate().isBlank()) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Template is required");
        }
    }

    /**
     * Process template by substituting parameters.
     */
    private String processTemplate(String template, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_PARAM_PATTERN.matcher(template);

        while (matcher.find()) {
            String paramName = matcher.group(1).trim();
            Object value = getNestedValue(parameters, paramName);

            if (value != null) {
                String replacement = formatValue(value);
                matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
            } else {
                // Keep original placeholder if parameter not provided
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Get nested value from parameters using dot notation.
     * E.g., "user.name" gets parameters.get("user").get("name")
     */
    @SuppressWarnings("unchecked")
    private Object getNestedValue(Map<String, Object> parameters, String path) {
        String[] parts = path.split("\\.");
        Object current = parameters;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(part);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }

        return current;
    }

    /**
     * Format a value for template substitution.
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    /**
     * Format output based on configuration.
     */
    private Object formatOutput(String result, TemplateConfig config) throws JsonProcessingException {
        if (config.getOutputFormat() == null) {
            return Map.of("result", result);
        }

        return switch (config.getOutputFormat()) {
            case TEXT -> result;
            case JSON -> objectMapper.readTree(result);
            case WRAPPED -> Map.of("result", result);
        };
    }
}
