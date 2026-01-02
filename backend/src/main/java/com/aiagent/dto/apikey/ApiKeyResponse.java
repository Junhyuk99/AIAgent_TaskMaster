package com.aiagent.dto.apikey;

import com.aiagent.entity.ApiKey;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private Long id;
    private String name;
    private String keyPrefix;
    private String description;
    private LocalDateTime lastUsedAt;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private Long usageCount;
    private Integer rateLimit;
    private LocalDateTime createdAt;

    public static ApiKeyResponse from(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix())
                .description(apiKey.getDescription())
                .lastUsedAt(apiKey.getLastUsedAt())
                .expiresAt(apiKey.getExpiresAt())
                .isActive(apiKey.getIsActive())
                .usageCount(apiKey.getUsageCount())
                .rateLimit(apiKey.getRateLimit())
                .createdAt(apiKey.getCreatedAt())
                .build();
    }
}
