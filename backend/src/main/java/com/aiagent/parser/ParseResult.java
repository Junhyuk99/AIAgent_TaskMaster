package com.aiagent.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {

    private String content;
    private Map<String, Object> metadata;
    private int pageCount;
    private List<String> sections;
    private long characterCount;
    private long wordCount;

    public static ParseResult of(String content) {
        return ParseResult.builder()
                .content(content)
                .metadata(Map.of())
                .pageCount(1)
                .sections(List.of())
                .characterCount(content.length())
                .wordCount(countWords(content))
                .build();
    }

    public static ParseResult of(String content, Map<String, Object> metadata) {
        return ParseResult.builder()
                .content(content)
                .metadata(metadata)
                .pageCount(1)
                .sections(List.of())
                .characterCount(content.length())
                .wordCount(countWords(content))
                .build();
    }

    private static long countWords(String content) {
        if (content == null || content.isBlank()) {
            return 0;
        }
        return content.split("\\s+").length;
    }
}
