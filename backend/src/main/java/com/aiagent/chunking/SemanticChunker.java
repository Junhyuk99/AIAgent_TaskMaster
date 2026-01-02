package com.aiagent.chunking;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class SemanticChunker implements ChunkingStrategy {

    // Pattern to match sentence endings
    private static final Pattern SENTENCE_PATTERN = Pattern.compile(
            "(?<=[.!?])\\s+(?=[A-Z가-힣])|(?<=[.!?])$"
    );

    @Override
    public ChunkingConfig.ChunkingStrategyType getStrategyType() {
        return ChunkingConfig.ChunkingStrategyType.SEMANTIC;
    }

    @Override
    public List<TextChunk> chunk(String text, ChunkingConfig config) {
        List<TextChunk> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        List<String> sentences = splitIntoSentences(text);
        int chunkSize = config.getChunkSize();
        int index = 0;
        int currentOffset = 0;

        StringBuilder currentChunk = new StringBuilder();
        int chunkStartOffset = 0;

        for (String sentence : sentences) {
            String trimmedSentence = sentence.trim();
            if (trimmedSentence.isEmpty()) {
                continue;
            }

            // If adding this sentence would exceed chunk size
            if (currentChunk.length() + trimmedSentence.length() + 1 > chunkSize) {
                // Save current chunk if it has content
                if (currentChunk.length() > 0) {
                    chunks.add(createChunk(currentChunk.toString(), index++, chunkStartOffset, config));
                }

                // Handle very long sentences
                if (trimmedSentence.length() > chunkSize) {
                    // Split long sentence at word boundaries
                    List<String> parts = splitLongSentence(trimmedSentence, chunkSize);
                    for (String part : parts) {
                        chunks.add(createChunk(part, index++, currentOffset, config));
                        currentOffset += part.length() + 1;
                    }
                    currentChunk = new StringBuilder();
                    chunkStartOffset = currentOffset;
                } else {
                    currentChunk = new StringBuilder(trimmedSentence);
                    chunkStartOffset = currentOffset;
                    currentOffset += trimmedSentence.length() + 1;
                }
            } else {
                // Add to current chunk
                if (currentChunk.length() > 0) {
                    currentChunk.append(" ");
                }
                currentChunk.append(trimmedSentence);
                currentOffset += trimmedSentence.length() + 1;
            }
        }

        // Add remaining content
        if (currentChunk.length() > 0) {
            chunks.add(createChunk(currentChunk.toString(), index, chunkStartOffset, config));
        }

        log.debug("Semantic chunking: {} characters -> {} chunks", text.length(), chunks.size());
        return chunks;
    }

    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);

        int lastEnd = 0;
        while (matcher.find()) {
            String sentence = text.substring(lastEnd, matcher.start()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            lastEnd = matcher.end();
        }

        // Add remaining text
        if (lastEnd < text.length()) {
            String remaining = text.substring(lastEnd).trim();
            if (!remaining.isEmpty()) {
                sentences.add(remaining);
            }
        }

        // If no sentences were found, treat the whole text as one sentence
        if (sentences.isEmpty() && !text.trim().isEmpty()) {
            sentences.add(text.trim());
        }

        return sentences;
    }

    private List<String> splitLongSentence(String sentence, int maxLength) {
        List<String> parts = new ArrayList<>();
        String[] words = sentence.split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            if (current.length() + word.length() + 1 > maxLength) {
                if (current.length() > 0) {
                    parts.add(current.toString().trim());
                    current = new StringBuilder();
                }
            }
            if (current.length() > 0) {
                current.append(" ");
            }
            current.append(word);
        }

        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }

        return parts;
    }

    private TextChunk createChunk(String content, int index, int startOffset, ChunkingConfig config) {
        return TextChunk.builder()
                .content(content.trim())
                .index(index)
                .startOffset(startOffset)
                .endOffset(startOffset + content.length())
                .metadata(Map.of(
                        "strategy", "SEMANTIC",
                        "chunkSize", config.getChunkSize()
                ))
                .build();
    }
}
