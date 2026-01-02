package com.aiagent.dto.apikey;

import com.aiagent.entity.ApiKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response returned when creating an API key.
 * Contains the full key which is only shown once.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreatedResponse {

    private Long id;
    private String name;
    private String key; // Full key - only shown at creation time!
    private String keyPrefix;
    private String description;
    private LocalDateTime expiresAt;
    private Integer rateLimit;
    private LocalDateTime createdAt;
    private String warning;

    public static ApiKeyCreatedResponse from(ApiKey apiKey, String fullKey) {
        return ApiKeyCreatedResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .key(fullKey)
                .keyPrefix(apiKey.getKeyPrefix())
                .description(apiKey.getDescription())
                .expiresAt(apiKey.getExpiresAt())
                .rateLimit(apiKey.getRateLimit())
                .createdAt(apiKey.getCreatedAt())
                .warning("This is the only time you will see this API key. Please save it securely.")
                .build();
    }
}
