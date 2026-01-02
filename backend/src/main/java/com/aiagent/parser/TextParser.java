package com.aiagent.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class TextParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "md", "markdown");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "text/plain",
            "text/markdown",
            "text/x-markdown"
    );

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    @Override
    public Set<String> getSupportedMimeTypes() {
        return SUPPORTED_MIME_TYPES;
    }

    @Override
    public ParseResult parse(InputStream inputStream, String fileName) throws Exception {
        String content;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            content = sb.toString().trim();
        }

        String extension = getExtension(fileName);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("encoding", "UTF-8");
        metadata.put("format", extension);

        List<String> sections = new ArrayList<>();
        if ("md".equals(extension) || "markdown".equals(extension)) {
            sections = extractMarkdownHeadings(content);
            metadata.put("isMarkdown", true);
        }

        log.info("Parsed text file: {} - {} characters",
                fileName, content.length());

        return ParseResult.builder()
                .content(content)
                .metadata(metadata)
                .pageCount(estimatePageCount(content))
                .sections(sections)
                .characterCount(content.length())
                .wordCount(countWords(content))
                .build();
    }

    private List<String> extractMarkdownHeadings(String content) {
        List<String> headings = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            if (line.startsWith("#")) {
                // Remove # symbols and trim
                String heading = line.replaceFirst("^#+\\s*", "").trim();
                if (!heading.isEmpty()) {
                    headings.add(heading);
                }
            }
        }
        return headings;
    }

    private String getExtension(String fileName) {
        if (fileName == null) return "txt";
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return "txt";
    }

    private int estimatePageCount(String content) {
        return Math.max(1, (int) Math.ceil(content.length() / 3000.0));
    }

    private long countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.split("\\s+").length;
    }
}
