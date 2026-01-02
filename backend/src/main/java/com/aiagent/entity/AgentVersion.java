package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_versions", indexes = {
        @Index(name = "idx_agent_version_agent", columnList = "agent_id"),
        @Index(name = "idx_agent_version_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private Agent agent;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "model_name")
    private String modelName;

    private Double temperature;

    @Column(name = "max_tokens")
    private Integer maxTokens;

    @Column(name = "llm_server_id")
    private Long llmServerId;

    @Column(name = "function_ids", columnDefinition = "TEXT")
    private String functionIds;

    @Column(name = "knowledge_base_ids", columnDefinition = "TEXT")
    private String knowledgeBaseIds;

    @Column(name = "change_summary")
    private String changeSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;
}
