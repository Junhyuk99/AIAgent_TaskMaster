package com.aiagent.chunking;

import java.util.List;

public interface ChunkingStrategy {

    ChunkingConfig.ChunkingStrategyType getStrategyType();

    List<TextChunk> chunk(String text, ChunkingConfig config);
}
