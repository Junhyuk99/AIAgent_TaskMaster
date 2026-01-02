package com.aiagent.service;

import com.aiagent.dto.UsageStatisticsResponse;
import com.aiagent.dto.UsageStatisticsResponse.*;
import com.aiagent.repository.ApiUsageRepository;
import com.aiagent.repository.ConversationRepository;
import com.aiagent.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsageStatisticsService {

    private final ApiUsageRepository apiUsageRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    public enum Period {
        DAY(1),
        WEEK(7),
        MONTH(30),
        QUARTER(90),
        YEAR(365);

        private final int days;

        Period(int days) {
            this.days = days;
        }

        public LocalDateTime getSince() {
            return LocalDateTime.now().minusDays(days);
        }
    }

    @Transactional(readOnly = true)
    public UsageStatisticsResponse getStatistics(Long userId, Period period) {
        LocalDateTime since = period.getSince();

        return UsageStatisticsResponse.builder()
                .overview(getOverviewStats(userId, since))
                .dailyStats(getDailyStats(userId, since))
                .agentStats(getAgentStats(userId, since))
                .modelStats(getModelStats(userId, since))
                .build();
    }

    @Transactional(readOnly = true)
    public UsageStatisticsResponse getStatistics(Long userId, LocalDateTime since, LocalDateTime until) {
        return UsageStatisticsResponse.builder()
                .overview(getOverviewStats(userId, since))
                .dailyStats(getDailyStats(userId, since))
                .agentStats(getAgentStats(userId, since))
                .modelStats(getModelStats(userId, since))
                .build();
    }

    private OverviewStats getOverviewStats(Long userId, LocalDateTime since) {
        long totalApiCalls = apiUsageRepository.countByUserIdSince(userId, since);
        long totalConversations = conversationRepository.countByUserIdSince(userId, since);
        long totalMessages = messageRepository.countByUserIdSince(userId, since);

        long totalTokens = apiUsageRepository.sumTotalTokensByUserIdSince(userId, since);
        long inputTokens = apiUsageRepository.sumInputTokensByUserIdSince(userId, since);
        long outputTokens = apiUsageRepository.sumOutputTokensByUserIdSince(userId, since);

        // If API usage doesn't have token data, fall back to message token counts
        if (totalTokens == 0) {
            totalTokens = messageRepository.sumTokensByUserIdSince(userId, since);
        }

        long errorCount = apiUsageRepository.countErrorsByUserIdSince(userId, since);
        double errorRate = totalApiCalls > 0 ? (double) errorCount / totalApiCalls * 100 : 0;

        return OverviewStats.builder()
                .totalApiCalls(totalApiCalls)
                .totalConversations(totalConversations)
                .totalMessages(totalMessages)
                .totalTokens(totalTokens)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .errorCount(errorCount)
                .errorRate(Math.round(errorRate * 100.0) / 100.0)
                .avgResponseTimeMs(0) // Can be calculated from apiUsage
                .build();
    }

    private List<DailyStats> getDailyStats(Long userId, LocalDateTime since) {
        // Get daily message stats
        List<Object[]> messageStats = messageRepository.getDailyStatsByUserIdSince(userId, since);
        Map<LocalDate, DailyStats> dailyMap = new LinkedHashMap<>();

        for (Object[] row : messageStats) {
            LocalDate date = row[0] instanceof java.sql.Date ?
                    ((java.sql.Date) row[0]).toLocalDate() :
                    (LocalDate) row[0];
            long messages = ((Number) row[1]).longValue();
            long tokens = ((Number) row[2]).longValue();

            dailyMap.put(date, DailyStats.builder()
                    .date(date)
                    .messages(messages)
                    .tokens(tokens)
                    .apiCalls(0)
                    .conversations(0)
                    .build());
        }

        // Merge with conversation stats
        List<Object[]> convStats = conversationRepository.countDailyByUserIdSince(userId, since);
        for (Object[] row : convStats) {
            LocalDate date = row[0] instanceof java.sql.Date ?
                    ((java.sql.Date) row[0]).toLocalDate() :
                    (LocalDate) row[0];
            long conversations = ((Number) row[1]).longValue();

            DailyStats existing = dailyMap.get(date);
            if (existing != null) {
                existing.setConversations(conversations);
            } else {
                dailyMap.put(date, DailyStats.builder()
                        .date(date)
                        .conversations(conversations)
                        .messages(0)
                        .tokens(0)
                        .apiCalls(0)
                        .build());
            }
        }

        // Merge with API call stats
        List<Object[]> apiStats = apiUsageRepository.countDailyByUserIdSince(userId, since);
        for (Object[] row : apiStats) {
            LocalDate date = row[0] instanceof java.sql.Date ?
                    ((java.sql.Date) row[0]).toLocalDate() :
                    (LocalDate) row[0];
            long apiCalls = ((Number) row[1]).longValue();

            DailyStats existing = dailyMap.get(date);
            if (existing != null) {
                existing.setApiCalls(apiCalls);
            } else {
                dailyMap.put(date, DailyStats.builder()
                        .date(date)
                        .apiCalls(apiCalls)
                        .conversations(0)
                        .messages(0)
                        .tokens(0)
                        .build());
            }
        }

        // Sort by date and return
        return dailyMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<AgentStats> getAgentStats(Long userId, LocalDateTime since) {
        List<Object[]> agentUsage = apiUsageRepository.getAgentUsageStatsByUserIdSince(userId, since);
        List<AgentStats> stats = new ArrayList<>();

        for (Object[] row : agentUsage) {
            Long agentId = ((Number) row[0]).longValue();
            String agentName = (String) row[1];
            long apiCalls = ((Number) row[2]).longValue();
            long tokens = ((Number) row[3]).longValue();

            // Get conversation count for this agent
            long conversations = conversationRepository.countByAgentIdSince(agentId, since);

            // Get average response time
            Double avgTime = apiUsageRepository.avgExecutionTimeByAgentIdSince(agentId, since);

            stats.add(AgentStats.builder()
                    .agentId(agentId)
                    .agentName(agentName)
                    .apiCalls(apiCalls)
                    .conversations(conversations)
                    .tokens(tokens)
                    .avgResponseTimeMs(avgTime != null ? Math.round(avgTime * 100.0) / 100.0 : 0)
                    .build());
        }

        return stats;
    }

    private List<ModelStats> getModelStats(Long userId, LocalDateTime since) {
        // Try API usage first
        List<Object[]> apiModelStats = apiUsageRepository.getModelUsageStatsByUserIdSince(userId, since);

        if (!apiModelStats.isEmpty()) {
            return apiModelStats.stream()
                    .map(row -> ModelStats.builder()
                            .modelName((String) row[0])
                            .requestCount(((Number) row[1]).longValue())
                            .totalTokens(((Number) row[2]).longValue())
                            .build())
                    .collect(Collectors.toList());
        }

        // Fall back to message model stats
        List<Object[]> messageModelStats = messageRepository.getModelStatsByUserIdSince(userId, since);
        return messageModelStats.stream()
                .map(row -> ModelStats.builder()
                        .modelName((String) row[0])
                        .requestCount(((Number) row[1]).longValue())
                        .totalTokens(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AgentStats> getAgentRanking(Long userId, Period period, int limit) {
        List<AgentStats> stats = getAgentStats(userId, period.getSince());
        return stats.stream()
                .sorted((a, b) -> Long.compare(b.getApiCalls(), a.getApiCalls()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OverviewStats getQuickStats(Long userId) {
        return getOverviewStats(userId, Period.WEEK.getSince());
    }
}
