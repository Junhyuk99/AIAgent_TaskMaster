package com.aiagent.dto.knowledge;

import com.aiagent.entity.KnowledgeBase;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseResponse {

    private Long id;
    private String name;
    private String description;
    private String collectionName;
    private Integer chunkSize;
    private Integer chunkOverlap;
    private KnowledgeBase.ChunkingStrategy chunkingStrategy;
    private Boolean isActive;
    private Integer documentCount;
    private List<DocumentResponse> documents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KnowledgeBaseResponse from(KnowledgeBase kb) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .collectionName(kb.getCollectionName())
                .chunkSize(kb.getChunkSize())
                .chunkOverlap(kb.getChunkOverlap())
                .chunkingStrategy(kb.getChunkingStrategy())
                .isActive(kb.getIsActive())
                .documentCount(kb.getDocuments() != null ? kb.getDocuments().size() : 0)
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }

    public static KnowledgeBaseResponse fromWithDocuments(KnowledgeBase kb) {
        return KnowledgeBaseResponse.builder()
                .id(kb.getId())
                .name(kb.getName())
                .description(kb.getDescription())
                .collectionName(kb.getCollectionName())
                .chunkSize(kb.getChunkSize())
                .chunkOverlap(kb.getChunkOverlap())
                .chunkingStrategy(kb.getChunkingStrategy())
                .isActive(kb.getIsActive())
                .documentCount(kb.getDocuments() != null ? kb.getDocuments().size() : 0)
                .documents(kb.getDocuments() != null ?
                        kb.getDocuments().stream()
                                .map(DocumentResponse::from)
                                .toList() : List.of())
                .createdAt(kb.getCreatedAt())
                .updatedAt(kb.getUpdatedAt())
                .build();
    }
}
