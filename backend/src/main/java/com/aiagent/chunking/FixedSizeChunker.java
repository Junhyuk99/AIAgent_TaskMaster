package com.aiagent.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FixedSizeChunker implements ChunkingStrategy {

    @Override
    public ChunkingConfig.ChunkingStrategyType getStrategyType() {
        return ChunkingConfig.ChunkingStrategyType.FIXED_SIZE;
    }

    @Override
    public List<TextChunk> chunk(String text, ChunkingConfig config) {
        List<TextChunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();
        int textLength = text.length();
        int index = 0;
        int startOffset = 0;

        while (startOffset < textLength) {
            int endOffset = Math.min(startOffset + chunkSize, textLength);

            // Try to break at word boundary if not at the end
            if (endOffset < textLength) {
                int lastSpace = text.lastIndexOf(' ', endOffset);
                if (lastSpace > startOffset) {
                    endOffset = lastSpace;
                }
            }

            String chunkContent = text.substring(startOffset, endOffset).trim();

            if (!chunkContent.isEmpty()) {
                chunks.add(TextChunk.builder()
                        .content(chunkContent)
                        .index(index++)
                        .startOffset(startOffset)
                        .endOffset(endOffset)
                        .metadata(Map.of(
                                "strategy", "FIXED_SIZE",
                                "chunkSize", chunkSize,
                                "overlap", overlap
                        ))
                        .build());
            }

            // Move to next chunk with overlap
            startOffset = endOffset - overlap;
            if (startOffset >= textLength || endOffset >= textLength) {
                break;
            }
        }

        log.debug("Fixed size chunking: {} characters -> {} chunks", textLength, chunks.size());
        return chunks;
    }
}
