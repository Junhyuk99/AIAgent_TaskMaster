package com.aiagent.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchRequest {

    private String query;

    @Builder.Default
    private Integer topK = 5;

    @Builder.Default
    private Double minScore = 0.5;

    private Long knowledgeBaseId;
}
