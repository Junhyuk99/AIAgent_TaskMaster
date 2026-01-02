package com.aiagent.dto.knowledge;

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
public class SearchResult {

    private String query;
    private List<SearchMatch> matches;
    private Long searchTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SearchMatch {
        private String chunkId;
        private String content;
        private String documentName;
        private Long documentId;
        private Double score;
        private Map<String, Object> metadata;
    }
}
