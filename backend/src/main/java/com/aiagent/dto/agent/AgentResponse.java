package com.aiagent.dto.agent;

import com.aiagent.entity.Agent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResponse {

    private Long id;
    private String name;
    private String slug;
    private String description;
    private String systemPrompt;
    private Long llmServerId;
    private String llmServerName;
    private String modelName;
    private Double temperature;
    private Integer maxTokens;
    private Boolean isActive;
    private Set<FunctionSummary> functions;
    private Set<KnowledgeBaseSummary> knowledgeBases;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionSummary {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KnowledgeBaseSummary {
        private Long id;
        private String name;
    }

    public static AgentResponse from(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .slug(agent.getSlug())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .llmServerId(agent.getLlmServer() != null ? agent.getLlmServer().getId() : null)
                .llmServerName(agent.getLlmServer() != null ? agent.getLlmServer().getName() : null)
                .modelName(agent.getModelName())
                .temperature(agent.getTemperature())
                .maxTokens(agent.getMaxTokens())
                .isActive(agent.getIsActive())
                .functions(agent.getFunctions() != null ?
                        agent.getFunctions().stream()
                                .map(f -> FunctionSummary.builder()
                                        .id(f.getId())
                                        .name(f.getName())
                                        .build())
                                .collect(Collectors.toSet()) : Set.of())
                .knowledgeBases(agent.getKnowledgeBases() != null ?
                        agent.getKnowledgeBases().stream()
                                .map(kb -> KnowledgeBaseSummary.builder()
                                        .id(kb.getId())
                                        .name(kb.getName())
                                        .build())
                                .collect(Collectors.toSet()) : Set.of())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }

    public static AgentResponse fromBasic(Agent agent) {
        return AgentResponse.builder()
                .id(agent.getId())
                .name(agent.getName())
                .slug(agent.getSlug())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .llmServerId(agent.getLlmServer() != null ? agent.getLlmServer().getId() : null)
                .llmServerName(agent.getLlmServer() != null ? agent.getLlmServer().getName() : null)
                .modelName(agent.getModelName())
                .temperature(agent.getTemperature())
                .maxTokens(agent.getMaxTokens())
                .isActive(agent.getIsActive())
                .createdAt(agent.getCreatedAt())
                .updatedAt(agent.getUpdatedAt())
                .build();
    }
}
