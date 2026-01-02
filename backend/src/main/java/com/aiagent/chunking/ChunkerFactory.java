package com.aiagent.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChunkerFactory {

    private final Map<ChunkingConfig.ChunkingStrategyType, ChunkingStrategy> chunkers = new HashMap<>();

    public ChunkerFactory(List<ChunkingStrategy> strategies) {
        for (ChunkingStrategy strategy : strategies) {
            chunkers.put(strategy.getStrategyType(), strategy);
        }
        log.info("ChunkerFactory initialized with {} chunking strategies", chunkers.size());
    }

    public ChunkingStrategy getChunker(ChunkingConfig.ChunkingStrategyType type) {
        ChunkingStrategy strategy = chunkers.get(type);
        if (strategy == null) {
            log.warn("No chunker found for type {}, falling back to FIXED_SIZE", type);
            strategy = chunkers.get(ChunkingConfig.ChunkingStrategyType.FIXED_SIZE);
        }
        return strategy;
    }

    public List<TextChunk> chunk(String text, ChunkingConfig config) {
        ChunkingStrategy strategy = getChunker(config.getStrategyType());
        return strategy.chunk(text, config);
    }
}
