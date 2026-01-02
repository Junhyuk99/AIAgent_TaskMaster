package com.aiagent.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResponse {

    private String formattedContext;

    private List<DocumentSource> sources;

    private List<RetrievedChunk> retrievedChunks;

    private long searchTimeMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentSource {
        private String documentId;
        private String documentName;
        private Integer chunkIndex;
        private Double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrievedChunk {
        private String chunkId;
        private String content;
        private Double score;
        private String documentId;
        private String documentName;
        private Integer chunkIndex;
    }
}
