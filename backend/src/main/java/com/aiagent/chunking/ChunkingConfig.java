package com.aiagent.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkingConfig {

    @Builder.Default
    private int chunkSize = 500;

    @Builder.Default
    private int chunkOverlap = 50;

    @Builder.Default
    private ChunkingStrategyType strategyType = ChunkingStrategyType.FIXED_SIZE;

    public enum ChunkingStrategyType {
        FIXED_SIZE,
        PARAGRAPH,
        SEMANTIC
    }
}
