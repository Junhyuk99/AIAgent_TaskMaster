package com.aiagent.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
public class DocxParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("docx", "doc");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/msword"
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
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            List<String> sections = new ArrayList<>();

            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    content.append(text).append("\n");

                    // Check if this is a heading (section)
                    String style = paragraph.getStyle();
                    if (style != null && (style.contains("Heading") || style.contains("Title"))) {
                        sections.add(text.trim());
                    }
                }
            }

            // Extract tables
            for (XWPFTable table : document.getTables()) {
                content.append("\n[Table]\n");
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cells.add(cell.getText());
                    }
                    content.append(String.join(" | ", cells)).append("\n");
                }
                content.append("\n");
            }

            Map<String, Object> metadata = extractMetadata(document);
            String contentStr = content.toString().trim();

            log.info("Parsed DOCX: {} - {} characters, {} sections",
                    fileName, contentStr.length(), sections.size());

            return ParseResult.builder()
                    .content(contentStr)
                    .metadata(metadata)
                    .pageCount(estimatePageCount(contentStr))
                    .sections(sections)
                    .characterCount(contentStr.length())
                    .wordCount(countWords(contentStr))
                    .build();
        }
    }

    private Map<String, Object> extractMetadata(XWPFDocument document) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            var coreProps = document.getProperties().getCoreProperties();
            if (coreProps != null) {
                if (coreProps.getTitle() != null) metadata.put("title", coreProps.getTitle());
                if (coreProps.getCreator() != null) metadata.put("author", coreProps.getCreator());
                if (coreProps.getSubject() != null) metadata.put("subject", coreProps.getSubject());
                if (coreProps.getKeywords() != null) metadata.put("keywords", coreProps.getKeywords());
                if (coreProps.getDescription() != null) metadata.put("description", coreProps.getDescription());
                if (coreProps.getCreated() != null) {
                    metadata.put("creationDate", coreProps.getCreated().toString());
                }
                if (coreProps.getModified() != null) {
                    metadata.put("modificationDate", coreProps.getModified().toString());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract metadata: {}", e.getMessage());
        }

        return metadata;
    }

    private int estimatePageCount(String content) {
        // Rough estimate: ~3000 characters per page
        return Math.max(1, (int) Math.ceil(content.length() / 3000.0));
    }

    private long countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.split("\\s+").length;
    }
}
