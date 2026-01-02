package com.aiagent.service;

import com.aiagent.entity.Agent;
import com.aiagent.entity.ApiKey;
import com.aiagent.entity.ApiUsage;
import com.aiagent.repository.ApiUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiUsageService {

    private final ApiUsageRepository apiUsageRepository;

    /**
     * Record API usage asynchronously.
     */
    @Async
    @Transactional
    public void recordUsage(ApiKey apiKey, Agent agent, String endpoint, String httpMethod,
                            Integer responseStatus, Long executionTimeMs, Long requestSize,
                            Long responseSize, String ipAddress, String userAgent, String errorMessage) {
        try {
            ApiUsage usage = ApiUsage.builder()
                    .apiKey(apiKey)
                    .agent(agent)
                    .endpoint(endpoint)
                    .httpMethod(httpMethod)
                    .responseStatus(responseStatus)
                    .executionTimeMs(executionTimeMs)
                    .requestSize(requestSize)
                    .responseSize(responseSize)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();

            apiUsageRepository.save(usage);
            log.debug("Recorded API usage for key {} on endpoint {}", apiKey.getName(), endpoint);
        } catch (Exception e) {
            log.error("Failed to record API usage: {}", e.getMessage());
        }
    }

    /**
     * Get usage for an API key.
     */
    @Transactional(readOnly = true)
    public Page<ApiUsage> getUsageByApiKey(Long apiKeyId, Pageable pageable) {
        return apiUsageRepository.findByApiKeyId(apiKeyId, pageable);
    }

    /**
     * Get usage for an API key since a given time.
     */
    @Transactional(readOnly = true)
    public List<ApiUsage> getUsageSince(Long apiKeyId, LocalDateTime since) {
        return apiUsageRepository.findByApiKeyIdSince(apiKeyId, since);
    }

    /**
     * Get usage count for an API key since a given time.
     */
    @Transactional(readOnly = true)
    public long getUsageCount(Long apiKeyId, LocalDateTime since) {
        return apiUsageRepository.countByApiKeyIdSince(apiKeyId, since);
    }

    /**
     * Get total execution time for an API key since a given time.
     */
    @Transactional(readOnly = true)
    public Long getTotalExecutionTime(Long apiKeyId, LocalDateTime since) {
        Long total = apiUsageRepository.sumExecutionTimeByApiKeyIdSince(apiKeyId, since);
        return total != null ? total : 0L;
    }

    /**
     * Get usage statistics for a user.
     */
    @Transactional(readOnly = true)
    public UsageStats getUsageStats(Long userId, LocalDateTime since) {
        long requestCount = apiUsageRepository.countByUserIdSince(userId, since);
        return UsageStats.builder()
                .requestCount(requestCount)
                .periodStart(since)
                .periodEnd(LocalDateTime.now())
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UsageStats {
        private long requestCount;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
    }
}
