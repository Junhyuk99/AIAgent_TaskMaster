package com.aiagent.chunking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ParagraphChunker implements ChunkingStrategy {

    private final FixedSizeChunker fixedSizeChunker;

    @Override
    public ChunkingConfig.ChunkingStrategyType getStrategyType() {
        return ChunkingConfig.ChunkingStrategyType.PARAGRAPH;
    }

    @Override
    public List<TextChunk> chunk(String text, ChunkingConfig config) {
        List<TextChunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Split by paragraph boundaries (one or more blank lines)
        String[] paragraphs = text.split("\\n\\s*\\n");
        int chunkSize = config.getChunkSize();
        int index = 0;
        int currentOffset = 0;

        StringBuilder currentChunk = new StringBuilder();
        int chunkStartOffset = 0;

        for (String paragraph : paragraphs) {
            String trimmedParagraph = paragraph.trim();
            if (trimmedParagraph.isEmpty()) {
                currentOffset += paragraph.length() + 2; // +2 for \n\n
                continue;
            }

            // If single paragraph is larger than chunk size, use fixed size chunker
            if (trimmedParagraph.length() > chunkSize) {
                // First, add any accumulated content
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString(), index++, chunkStartOffset, config));
                    currentChunk = new StringBuilder();
                }

                // Then chunk the large paragraph
                List<TextChunk> subChunks = fixedSizeChunker.chunk(trimmedParagraph, config);
                for (TextChunk subChunk : subChunks) {
                    chunks.add(TextChunk.builder()
                            .content(subChunk.getContent())
                            .index(index++)
                            .startOffset(currentOffset + subChunk.getStartOffset())
                            .endOffset(currentOffset + subChunk.getEndOffset())
                            .metadata(Map.of(
                                    "strategy", "PARAGRAPH",
                                    "subChunked", true
                            ))
                            .build());
                }
                currentOffset += trimmedParagraph.length() + 2;
                chunkStartOffset = currentOffset;
                continue;
            }

            // Check if adding this paragraph would exceed chunk size
            if (currentChunk.length() + trimmedParagraph.length() + 1 > chunkSize) {
                // Save current chunk
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString(), index++, chunkStartOffset, config));
                }
                currentChunk = new StringBuilder(trimmedParagraph);
                chunkStartOffset = currentOffset;
            } else {
                // Add to current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                }
                currentChunk.append(trimmedParagraph);
            }

            currentOffset += paragraph.length() + 2;
        }

        // Add remaining content
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), index, chunkStartOffset, config));
        }

        log.debug("Paragraph chunking: {} characters -> {} chunks", text.length(), chunks.size());
        return chunks;
    }

    private TextChunk createChunk(String content, int index, int startOffset, ChunkingConfig config) {
        return TextChunk.builder()
                .content(content.trim())
                .index(index)
                .startOffset(startOffset)
                .endOffset(startOffset + content.length())
                .metadata(Map.of(
                        "strategy", "PARAGRAPH",
                        "chunkSize", config.getChunkSize()
                ))
                .build();
    }
}
