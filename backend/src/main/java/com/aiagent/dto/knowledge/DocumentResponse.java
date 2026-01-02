package com.aiagent.dto.knowledge;

import com.aiagent.entity.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String fileName;
    private Long fileSize;
    private String contentType;
    private Document.DocumentStatus status;
    private Integer chunkCount;
    private Integer progress;
    private String processingStage;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static DocumentResponse from(Document doc) {
        return DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .fileSize(doc.getFileSize())
                .contentType(doc.getContentType())
                .status(doc.getStatus())
                .chunkCount(doc.getChunkCount())
                .progress(doc.getProgress())
                .processingStage(doc.getProcessingStage())
                .errorMessage(doc.getErrorMessage())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
