package com.aiagent.executor.http;

import com.aiagent.executor.FunctionExecutionException;
import com.aiagent.executor.FunctionExecutionResult;
import com.aiagent.executor.FunctionExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Executor for HTTP API type functions.
 * Supports various HTTP methods, authentication schemes, templating, and response transformation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpApiFunctionExecutor implements FunctionExecutor {

    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    // Pattern for URL parameter substitution: {paramName}
    private static final Pattern URL_PARAM_PATTERN = Pattern.compile("\\{([^}]+)}");

    // Pattern for body template substitution: {{paramName}}
    private static final Pattern BODY_PARAM_PATTERN = Pattern.compile("\\{\\{([^}]+)}}");

    @Override
    public FunctionExecutionResult execute(String configJson, Map<String, Object> parameters) throws FunctionExecutionException {
        long startTime = System.currentTimeMillis();

        try {
            HttpApiConfig config = parseConfig(configJson);
            validateConfig(config);

            String processedUrl = processUrlTemplate(config.getUrl(), parameters);
            processedUrl = appendApiKeyToUrl(processedUrl, config);

            HttpHeaders headers = buildHeaders(config, parameters);
            Object body = buildRequestBody(config, parameters);

            log.debug("Executing HTTP {} request to: {}", config.getMethod(), processedUrl);

            Object response = executeRequest(config, processedUrl, headers, body);
            Object transformedResponse = transformResponse(response, config);

            long executionTime = System.currentTimeMillis() - startTime;

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("url", processedUrl);
            metadata.put("method", config.getMethod());
            metadata.put("statusCode", 200);

            return FunctionExecutionResult.success(transformedResponse, executionTime, metadata);

        } catch (FunctionExecutionException e) {
            throw e;
        } catch (WebClientResponseException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            String errorMessage = String.format("HTTP %d: %s", e.getStatusCode().value(), e.getStatusText());

            try {
                JsonNode errorBody = objectMapper.readTree(e.getResponseBodyAsString());
                if (errorBody.has("message")) {
                    errorMessage = errorBody.get("message").asText();
                } else if (errorBody.has("error")) {
                    errorMessage = errorBody.get("error").asText();
                }
            } catch (Exception ignored) {
                // Use default error message
            }

            log.error("HTTP request failed: {}", errorMessage);
            return FunctionExecutionResult.failure(errorMessage, executionTime);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("Function execution failed: {}", e.getMessage(), e);
            return FunctionExecutionResult.failure(e.getMessage(), executionTime);
        }
    }

    @Override
    public void validateConfig(String configJson) throws IllegalArgumentException {
        try {
            HttpApiConfig config = parseConfig(configJson);
            validateConfig(config);
        } catch (FunctionExecutionException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private HttpApiConfig parseConfig(String configJson) throws FunctionExecutionException {
        try {
            return objectMapper.readValue(configJson, HttpApiConfig.class);
        } catch (JsonProcessingException e) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Failed to parse configuration: " + e.getMessage(), e);
        }
    }

    private void validateConfig(HttpApiConfig config) throws FunctionExecutionException {
        if (config.getUrl() == null || config.getUrl().isBlank()) {
            throw new FunctionExecutionException("INVALID_CONFIG", "URL is required");
        }

        String method = config.getMethod().toUpperCase();
        if (!Set.of("GET", "POST", "PUT", "DELETE", "PATCH").contains(method)) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Unsupported HTTP method: " + method);
        }

        if (config.getTimeoutSeconds() < 1 || config.getTimeoutSeconds() > 300) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Timeout must be between 1 and 300 seconds");
        }

        // Validate URL format
        try {
            String testUrl = config.getUrl().replaceAll(URL_PARAM_PATTERN.pattern(), "test");
            new URI(testUrl);
        } catch (Exception e) {
            throw new FunctionExecutionException("INVALID_CONFIG", "Invalid URL format: " + config.getUrl());
        }
    }

    /**
     * Process URL template by substituting parameters.
     * Supports {paramName} syntax for path parameters.
     */
    private String processUrlTemplate(String urlTemplate, Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return urlTemplate;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = URL_PARAM_PATTERN.matcher(urlTemplate);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = parameters.get(paramName);

            if (value != null) {
                String encodedValue = URLEncoder.encode(String.valueOf(value), StandardCharsets.UTF_8);
                matcher.appendReplacement(result, encodedValue);
            } else {
                // Keep original placeholder if parameter not provided
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Append API key to URL if configured as query parameter.
     */
    private String appendApiKeyToUrl(String url, HttpApiConfig config) {
        if (config.getAuth() == null) return url;

        HttpApiConfig.AuthConfig auth = config.getAuth();
        if (auth.getType() == HttpApiConfig.AuthType.API_KEY &&
            auth.isApiKeyAsQuery() &&
            auth.getApiKey() != null) {

            String separator = url.contains("?") ? "&" : "?";
            String paramName = auth.getApiKeyQueryParam() != null ? auth.getApiKeyQueryParam() : "api_key";
            return url + separator + paramName + "=" +
                   URLEncoder.encode(auth.getApiKey(), StandardCharsets.UTF_8);
        }

        return url;
    }

    /**
     * Build HTTP headers including authentication and custom headers.
     */
    private HttpHeaders buildHeaders(HttpApiConfig config, Map<String, Object> parameters) {
        HttpHeaders headers = new HttpHeaders();

        // Set content type
        if (config.getContentType() != null) {
            headers.setContentType(MediaType.parseMediaType(config.getContentType()));
        }

        // Add static headers from config
        if (config.getHeaders() != null) {
            config.getHeaders().forEach((key, value) -> {
                // Process header value templates
                String processedValue = processBodyTemplate(value, parameters);
                headers.add(key, processedValue);
            });
        }

        // Add authentication headers
        if (config.getAuth() != null) {
            addAuthHeaders(headers, config.getAuth());
        }

        return headers;
    }

    /**
     * Add authentication headers based on auth configuration.
     */
    private void addAuthHeaders(HttpHeaders headers, HttpApiConfig.AuthConfig auth) {
        switch (auth.getType()) {
            case BASIC:
                if (auth.getUsername() != null && auth.getPassword() != null) {
                    String credentials = auth.getUsername() + ":" + auth.getPassword();
                    String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
                    headers.set(HttpHeaders.AUTHORIZATION, "Basic " + encoded);
                }
                break;

            case BEARER:
                if (auth.getToken() != null) {
                    headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + auth.getToken());
                }
                break;

            case API_KEY:
                if (auth.getApiKey() != null && !auth.isApiKeyAsQuery()) {
                    String headerName = auth.getApiKeyHeader() != null ? auth.getApiKeyHeader() : "X-API-Key";
                    headers.set(headerName, auth.getApiKey());
                }
                break;

            case NONE:
            default:
                // No authentication
                break;
        }
    }

    /**
     * Build request body from template or parameters.
     */
    private Object buildRequestBody(HttpApiConfig config, Map<String, Object> parameters) throws FunctionExecutionException {
        String method = config.getMethod().toUpperCase();

        // GET and DELETE typically don't have body
        if ("GET".equals(method) || "DELETE".equals(method)) {
            return null;
        }

        // If body template is provided, use it
        if (config.getBodyTemplate() != null && !config.getBodyTemplate().isBlank()) {
            String processedBody = processBodyTemplate(config.getBodyTemplate(), parameters);

            // Try to parse as JSON if content type is JSON
            if (config.getContentType() != null && config.getContentType().contains("json")) {
                try {
                    return objectMapper.readTree(processedBody);
                } catch (JsonProcessingException e) {
                    // Return as string if not valid JSON
                    return processedBody;
                }
            }
            return processedBody;
        }

        // Otherwise, send parameters as JSON body
        return parameters != null ? parameters : Map.of();
    }

    /**
     * Process body template by substituting parameters.
     * Supports {{paramName}} syntax.
     */
    private String processBodyTemplate(String template, Map<String, Object> parameters) {
        if (template == null || parameters == null || parameters.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = BODY_PARAM_PATTERN.matcher(template);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            Object value = parameters.get(paramName);

            if (value != null) {
                String replacement;
                if (value instanceof String) {
                    // Escape JSON string values
                    replacement = value.toString().replace("\\", "\\\\").replace("\"", "\\\"");
                } else {
                    try {
                        replacement = objectMapper.writeValueAsString(value);
                        // Remove surrounding quotes for non-string values in JSON context
                        if (replacement.startsWith("\"") && replacement.endsWith("\"")) {
                            replacement = replacement.substring(1, replacement.length() - 1);
                        }
                    } catch (JsonProcessingException e) {
                        replacement = String.valueOf(value);
                    }
                }
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
     * Execute the HTTP request with retry support.
     */
    private Object executeRequest(HttpApiConfig config, String url, HttpHeaders headers, Object body) {
        WebClient webClient = webClientBuilder.build();
        HttpMethod httpMethod = HttpMethod.valueOf(config.getMethod().toUpperCase());

        WebClient.RequestBodySpec requestSpec = webClient
                .method(httpMethod)
                .uri(url)
                .headers(h -> h.addAll(headers));

        WebClient.ResponseSpec responseSpec;
        if (body != null) {
            responseSpec = requestSpec.bodyValue(body).retrieve();
        } else {
            responseSpec = requestSpec.retrieve();
        }

        Mono<Object> responseMono = responseSpec
                .onStatus(HttpStatusCode::isError, response ->
                    response.bodyToMono(String.class)
                            .flatMap(errorBody -> Mono.error(
                                WebClientResponseException.create(
                                    response.statusCode().value(),
                                    "HTTP Error",
                                    response.headers().asHttpHeaders(),
                                    errorBody.getBytes(StandardCharsets.UTF_8),
                                    StandardCharsets.UTF_8
                                )
                            ))
                )
                .bodyToMono(Object.class)
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()));

        // Apply retry logic if configured
        if (config.getRetryCount() > 0) {
            responseMono = responseMono.retryWhen(
                Retry.backoff(config.getRetryCount(), Duration.ofMillis(config.getRetryDelayMs()))
                     .filter(throwable -> isRetryable(throwable))
                     .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
            );
        }

        return responseMono.block();
    }

    /**
     * Determine if an exception is retryable.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof WebClientResponseException ex) {
            int status = ex.getStatusCode().value();
            // Retry on server errors (5xx) and some client errors
            return status >= 500 || status == 429; // 429 = Too Many Requests
        }
        return throwable instanceof java.io.IOException ||
               throwable instanceof java.util.concurrent.TimeoutException;
    }

    /**
     * Transform the response based on configuration.
     */
    private Object transformResponse(Object response, HttpApiConfig config) {
        if (response == null || config.getResponseTransform() == null) {
            return response;
        }

        HttpApiConfig.ResponseTransform transform = config.getResponseTransform();

        // Apply JSON path extraction if configured
        if (transform.getJsonPath() != null && !transform.getJsonPath().isBlank()) {
            try {
                String jsonString = objectMapper.writeValueAsString(response);
                Object extracted = JsonPath.read(jsonString, transform.getJsonPath());

                // Unwrap single-element arrays if configured
                if (transform.isUnwrapArrays() && extracted instanceof List) {
                    List<?> list = (List<?>) extracted;
                    if (list.size() == 1) {
                        return list.get(0);
                    }
                }

                return extracted;
            } catch (PathNotFoundException e) {
                log.warn("JSON path not found: {}", transform.getJsonPath());
                return null;
            } catch (Exception e) {
                log.warn("Failed to apply JSON path transformation: {}", e.getMessage());
                return response;
            }
        }

        return response;
    }
}
