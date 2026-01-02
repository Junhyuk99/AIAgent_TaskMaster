package com.aiagent.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class CsvParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("csv");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "text/csv",
            "application/csv"
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
        StringBuilder content = new StringBuilder();
        List<String> headers = new ArrayList<>();
        int rowCount = 0;

        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            // Get headers
            headers.addAll(csvParser.getHeaderNames());

            // Process each row
            for (CSVRecord record : csvParser) {
                rowCount++;
                List<String> values = new ArrayList<>();
                for (int i = 0; i < headers.size(); i++) {
                    String header = headers.get(i);
                    String value = i < record.size() ? record.get(i) : "";
                    values.add(header + ": " + value);
                }
                content.append(String.join(", ", values)).append("\n");
            }
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("headers", headers);
        metadata.put("rowCount", rowCount);
        metadata.put("columnCount", headers.size());
        metadata.put("format", "CSV");

        String contentStr = content.toString().trim();

        log.info("Parsed CSV: {} - {} rows, {} columns",
                fileName, rowCount, headers.size());

        return ParseResult.builder()
                .content(contentStr)
                .metadata(metadata)
                .pageCount(1)
                .sections(headers)
                .characterCount(contentStr.length())
                .wordCount(countWords(contentStr))
                .build();
    }

    private long countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.split("\\s+").length;
    }
}
