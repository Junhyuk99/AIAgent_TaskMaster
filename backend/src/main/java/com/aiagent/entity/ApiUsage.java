package com.aiagent.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Tracks API usage for each request.
 */
@Entity
@Table(name = "api_usage", indexes = {
        @Index(name = "idx_api_usage_api_key", columnList = "api_key_id"),
        @Index(name = "idx_api_usage_agent", columnList = "agent_id"),
        @Index(name = "idx_api_usage_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private Agent agent;

    @Column(nullable = false)
    private String endpoint;

    @Column(name = "http_method", nullable = false)
    private String httpMethod;

    @Column(name = "response_status", nullable = false)
    private Integer responseStatus;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "request_size")
    private Long requestSize;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "model_used")
    private String modelUsed;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime timestamp = LocalDateTime.now();
}
