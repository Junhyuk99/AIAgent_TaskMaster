package com.aiagent.dto;

import com.aiagent.entity.AgentVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVersionResponse {

    private Long id;
    private Long agentId;
    private Integer versionNumber;
    private String name;
    private String description;
    private String systemPrompt;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Long llmServerId;
    private List<Long> functionIds;
    private List<Long> knowledgeBaseIds;
    private String changeSummary;
    private String createdByName;
    private LocalDateTime createdAt;
    private Boolean isCurrent;

    public static AgentVersionResponse fromEntity(AgentVersion version) {
        return AgentVersionResponse.builder()
                .id(version.getId())
                .agentId(version.getAgent().getId())
                .versionNumber(version.getVersionNumber())
                .name(version.getName())
                .description(version.getDescription())
                .systemPrompt(version.getSystemPrompt())
                .modelName(version.getModelName())
                .temperature(version.getTemperature())
                .maxTokens(version.getMaxTokens())
                .llmServerId(version.getLlmServerId())
                .functionIds(parseIds(version.getFunctionIds()))
                .knowledgeBaseIds(parseIds(version.getKnowledgeBaseIds()))
                .changeSummary(version.getChangeSummary())
                .createdByName(version.getCreatedBy() != null ? version.getCreatedBy().getName() : null)
                .createdAt(version.getCreatedAt())
                .isCurrent(version.getIsCurrent())
                .build();
    }

    private static List<Long> parseIds(String ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .toList();
    }
}
