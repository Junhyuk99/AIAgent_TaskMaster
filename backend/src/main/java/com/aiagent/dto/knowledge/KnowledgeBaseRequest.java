package com.aiagent.dto.knowledge;

import com.aiagent.entity.KnowledgeBase;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeBaseRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Builder.Default
    @Min(value = 100, message = "Chunk size must be at least 100")
    @Max(value = 8000, message = "Chunk size must not exceed 8000")
    private Integer chunkSize = 500;

    @Builder.Default
    @Min(value = 0, message = "Chunk overlap must be at least 0")
    @Max(value = 1000, message = "Chunk overlap must not exceed 1000")
    private Integer chunkOverlap = 50;

    @Builder.Default
    private KnowledgeBase.ChunkingStrategy chunkingStrategy = KnowledgeBase.ChunkingStrategy.PARAGRAPH;
}
