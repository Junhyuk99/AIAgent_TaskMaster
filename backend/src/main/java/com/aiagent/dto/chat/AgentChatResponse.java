package com.aiagent.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {

    private String conversationId;
    private String response;
    private String model;
    private Long agentId;
    private String agentName;
    private List<DocumentSource> sources;
    private List<FunctionExecution> functionExecutions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSource {
        private String documentId;
        private String documentName;
        private Double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunctionExecution {
        private String functionName;
        private Map<String, Object> arguments;
        private String result;
        private String error;
        private long executionTimeMs;
    }
}
