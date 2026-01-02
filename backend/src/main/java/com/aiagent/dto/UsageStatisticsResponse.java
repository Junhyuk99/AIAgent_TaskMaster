package com.aiagent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageStatisticsResponse {

    private OverviewStats overview;
    private List<DailyStats> dailyStats;
    private List<AgentStats> agentStats;
    private List<ModelStats> modelStats;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewStats {
        private long totalApiCalls;
        private long totalConversations;
        private long totalMessages;
        private long totalTokens;
        private long inputTokens;
        private long outputTokens;
        private long errorCount;
        private double errorRate;
        private double avgResponseTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private LocalDate date;
        private long apiCalls;
        private long conversations;
        private long messages;
        private long tokens;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentStats {
        private Long agentId;
        private String agentName;
        private long apiCalls;
        private long conversations;
        private long tokens;
        private double avgResponseTimeMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModelStats {
        private String modelName;
        private long requestCount;
        private long totalTokens;
    }
}
