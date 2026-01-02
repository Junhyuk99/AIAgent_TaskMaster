package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "llm_servers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LlmServer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LlmType type;

    @Column(name = "base_url", nullable = false)
    private String baseUrl;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum LlmType {
        OLLAMA, VLLM, OPENAI_COMPATIBLE, CUSTOM
    }
}
