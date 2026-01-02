package com.aiagent.executor.http;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for HTTP API function execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpApiConfig {

    /**
     * The HTTP method to use (GET, POST, PUT, DELETE, PATCH).
     */
    @Builder.Default
    private String method = "GET";

    /**
     * The URL template. Supports parameter substitution using {paramName} syntax.
     * Example: "https://api.example.com/users/{userId}"
     */
    private String url;

    /**
     * Static headers to include in the request.
     * These are merged with dynamic headers.
     */
    @Builder.Default
    private Map<String, String> headers = new HashMap<>();

    /**
     * Authentication configuration.
     */
    private AuthConfig auth;

    /**
     * Request body template for POST/PUT/PATCH requests.
     * Supports parameter substitution using {{paramName}} syntax.
     * If null, parameters are sent as JSON body.
     */
    private String bodyTemplate;

    /**
     * Content type for the request body.
     */
    @Builder.Default
    private String contentType = "application/json";

    /**
     * Request timeout in seconds.
     */
    @Builder.Default
    private int timeoutSeconds = 30;

    /**
     * Number of retry attempts on transient failures.
     */
    @Builder.Default
    private int retryCount = 0;

    /**
     * Delay between retries in milliseconds.
     */
    @Builder.Default
    private int retryDelayMs = 1000;

    /**
     * Response transformation configuration.
     */
    private ResponseTransform responseTransform;

    /**
     * Whether to follow redirects.
     */
    @Builder.Default
    private boolean followRedirects = true;

    /**
     * Whether to validate SSL certificates.
     */
    @Builder.Default
    private boolean validateSsl = true;

    /**
     * Authentication configuration options.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthConfig {
        /**
         * Authentication type: NONE, BASIC, BEARER, API_KEY, OAUTH2
         */
        @Builder.Default
        private AuthType type = AuthType.NONE;

        /**
         * Username for BASIC auth.
         */
        private String username;

        /**
         * Password for BASIC auth.
         */
        private String password;

        /**
         * Token for BEARER auth.
         */
        private String token;

        /**
         * API key value for API_KEY auth.
         */
        private String apiKey;

        /**
         * Header name for API_KEY auth (default: X-API-Key).
         */
        @Builder.Default
        private String apiKeyHeader = "X-API-Key";

        /**
         * Whether to send API key as query parameter instead of header.
         */
        @Builder.Default
        private boolean apiKeyAsQuery = false;

        /**
         * Query parameter name for API key (default: api_key).
         */
        @Builder.Default
        private String apiKeyQueryParam = "api_key";
    }

    /**
     * Authentication types supported.
     */
    public enum AuthType {
        NONE,
        BASIC,
        BEARER,
        API_KEY,
        OAUTH2
    }

    /**
     * Response transformation configuration.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResponseTransform {
        /**
         * JSON path to extract from response (e.g., "$.data.items").
         */
        private String jsonPath;

        /**
         * Expected response type: JSON, TEXT, BINARY.
         */
        @Builder.Default
        private ResponseType type = ResponseType.JSON;

        /**
         * Whether to unwrap single-element arrays.
         */
        @Builder.Default
        private boolean unwrapArrays = false;
    }

    /**
     * Response types supported.
     */
    public enum ResponseType {
        JSON,
        TEXT,
        BINARY
    }
}
