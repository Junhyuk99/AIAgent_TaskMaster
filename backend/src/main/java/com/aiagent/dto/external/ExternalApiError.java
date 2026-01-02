package com.aiagent.dto.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standardized error response for external API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExternalApiError {

    private String error;
    private String message;
    private String code;
    private Integer status;
    private LocalDateTime timestamp;
    private String path;

    public static ExternalApiError of(String error, String message, String code, Integer status, String path) {
        return ExternalApiError.builder()
                .error(error)
                .message(message)
                .code(code)
                .status(status)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    public static ExternalApiError unauthorized(String message, String path) {
        return of("Unauthorized", message, "UNAUTHORIZED", 401, path);
    }

    public static ExternalApiError forbidden(String message, String path) {
        return of("Forbidden", message, "FORBIDDEN", 403, path);
    }

    public static ExternalApiError notFound(String message, String path) {
        return of("Not Found", message, "NOT_FOUND", 404, path);
    }

    public static ExternalApiError badRequest(String message, String path) {
        return of("Bad Request", message, "BAD_REQUEST", 400, path);
    }

    public static ExternalApiError internalError(String message, String path) {
        return of("Internal Server Error", message, "INTERNAL_ERROR", 500, path);
    }

    public static ExternalApiError rateLimited(String message, String path) {
        return of("Too Many Requests", message, "RATE_LIMITED", 429, path);
    }
}
