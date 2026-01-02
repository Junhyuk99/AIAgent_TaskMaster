package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "agents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "system_prompt", columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "model_name")
    private String modelName;

    @Builder.Default
    private Double temperature = 0.7;

    @Column(name = "max_tokens")
    @Builder.Default
    private Integer maxTokens = 2048;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_server_id")
    private LlmServer llmServer;

    @ManyToMany
    @JoinTable(
        name = "agent_functions",
        joinColumns = @JoinColumn(name = "agent_id"),
        inverseJoinColumns = @JoinColumn(name = "function_id")
    )
    @Builder.Default
    private Set<Function> functions = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "agent_knowledge_bases",
        joinColumns = @JoinColumn(name = "agent_id"),
        inverseJoinColumns = @JoinColumn(name = "knowledge_base_id")
    )
    @Builder.Default
    private Set<KnowledgeBase> knowledgeBases = new HashSet<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AgentVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "agent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Conversation> conversations = new ArrayList<>();
}
