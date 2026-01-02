package com.aiagent.dto.llm;

import com.aiagent.entity.LlmServer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmServerResponse {

    private Long id;
    private String name;
    private LlmServer.LlmType type;
    private String baseUrl;
    private Boolean hasApiKey;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LlmServerResponse from(LlmServer server) {
        return LlmServerResponse.builder()
                .id(server.getId())
                .name(server.getName())
                .type(server.getType())
                .baseUrl(server.getBaseUrl())
                .hasApiKey(server.getApiKey() != null && !server.getApiKey().isEmpty())
                .isActive(server.getIsActive())
                .createdAt(server.getCreatedAt())
                .updatedAt(server.getUpdatedAt())
                .build();
    }
}
