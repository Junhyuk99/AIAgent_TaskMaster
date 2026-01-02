package com.aiagent.repository;

import com.aiagent.entity.ApiUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ApiUsageRepository extends JpaRepository<ApiUsage, Long> {

    List<ApiUsage> findByApiKeyId(Long apiKeyId);

    Page<ApiUsage> findByApiKeyId(Long apiKeyId, Pageable pageable);

    List<ApiUsage> findByAgentId(Long agentId);

    @Query("SELECT u FROM ApiUsage u WHERE u.apiKey.id = :apiKeyId AND u.timestamp >= :since ORDER BY u.timestamp DESC")
    List<ApiUsage> findByApiKeyIdSince(@Param("apiKeyId") Long apiKeyId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM ApiUsage u WHERE u.apiKey.id = :apiKeyId AND u.timestamp >= :since")
    long countByApiKeyIdSince(@Param("apiKeyId") Long apiKeyId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(u) FROM ApiUsage u WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT SUM(u.executionTimeMs) FROM ApiUsage u WHERE u.apiKey.id = :apiKeyId AND u.timestamp >= :since")
    Long sumExecutionTimeByApiKeyIdSince(@Param("apiKeyId") Long apiKeyId, @Param("since") LocalDateTime since);

    // Token usage queries
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM ApiUsage u WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since")
    Long sumTotalTokensByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(u.inputTokens), 0) FROM ApiUsage u WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since")
    Long sumInputTokensByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(u.outputTokens), 0) FROM ApiUsage u WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since")
    Long sumOutputTokensByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Agent-level statistics
    @Query("SELECT COUNT(u) FROM ApiUsage u WHERE u.agent.id = :agentId AND u.timestamp >= :since")
    long countByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM ApiUsage u WHERE u.agent.id = :agentId AND u.timestamp >= :since")
    Long sumTotalTokensByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(AVG(u.executionTimeMs), 0) FROM ApiUsage u WHERE u.agent.id = :agentId AND u.timestamp >= :since")
    Double avgExecutionTimeByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    // Daily aggregation queries
    @Query(value = "SELECT DATE(u.timestamp) as date, COUNT(u) as count FROM ApiUsage u " +
           "WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since " +
           "GROUP BY DATE(u.timestamp) ORDER BY date")
    List<Object[]> countDailyByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE(u.timestamp) as date, COALESCE(SUM(u.totalTokens), 0) as tokens FROM ApiUsage u " +
           "WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since " +
           "GROUP BY DATE(u.timestamp) ORDER BY date")
    List<Object[]> sumDailyTokensByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Model usage statistics
    @Query("SELECT u.modelUsed, COUNT(u), COALESCE(SUM(u.totalTokens), 0) FROM ApiUsage u " +
           "WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since AND u.modelUsed IS NOT NULL " +
           "GROUP BY u.modelUsed ORDER BY COUNT(u) DESC")
    List<Object[]> getModelUsageStatsByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Agent usage ranking
    @Query("SELECT u.agent.id, u.agent.name, COUNT(u), COALESCE(SUM(u.totalTokens), 0) FROM ApiUsage u " +
           "WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since AND u.agent IS NOT NULL " +
           "GROUP BY u.agent.id, u.agent.name ORDER BY COUNT(u) DESC")
    List<Object[]> getAgentUsageStatsByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Error rate
    @Query("SELECT COUNT(u) FROM ApiUsage u WHERE u.apiKey.user.id = :userId AND u.timestamp >= :since AND u.responseStatus >= 400")
    long countErrorsByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
