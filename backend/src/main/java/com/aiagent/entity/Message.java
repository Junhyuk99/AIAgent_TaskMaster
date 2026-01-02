package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "model_used")
    private String modelUsed;

    @Column(name = "function_calls", columnDefinition = "TEXT")
    private String functionCalls;

    @Column(name = "sources", columnDefinition = "TEXT")
    private String sources;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    public enum Role {
        USER,
        ASSISTANT,
        SYSTEM,
        TOOL
    }
}
