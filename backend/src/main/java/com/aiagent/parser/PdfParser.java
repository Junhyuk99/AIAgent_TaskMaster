package com.aiagent.parser;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Component
public class PdfParser implements DocumentParser {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf");
    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of("application/pdf");

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
        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String content = stripper.getText(document);
            content = cleanText(content);

            Map<String, Object> metadata = extractMetadata(document);
            int pageCount = document.getNumberOfPages();

            List<String> sections = extractSections(content);

            log.info("Parsed PDF: {} - {} pages, {} characters",
                    fileName, pageCount, content.length());

            return ParseResult.builder()
                    .content(content)
                    .metadata(metadata)
                    .pageCount(pageCount)
                    .sections(sections)
                    .characterCount(content.length())
                    .wordCount(countWords(content))
                    .build();
        }
    }

    private Map<String, Object> extractMetadata(PDDocument document) {
        Map<String, Object> metadata = new HashMap<>();
        PDDocumentInformation info = document.getDocumentInformation();

        if (info != null) {
            if (info.getTitle() != null) metadata.put("title", info.getTitle());
            if (info.getAuthor() != null) metadata.put("author", info.getAuthor());
            if (info.getSubject() != null) metadata.put("subject", info.getSubject());
            if (info.getKeywords() != null) metadata.put("keywords", info.getKeywords());
            if (info.getCreator() != null) metadata.put("creator", info.getCreator());
            if (info.getProducer() != null) metadata.put("producer", info.getProducer());
            if (info.getCreationDate() != null) {
                metadata.put("creationDate", info.getCreationDate().getTime().toString());
            }
            if (info.getModificationDate() != null) {
                metadata.put("modificationDate", info.getModificationDate().getTime().toString());
            }
        }

        return metadata;
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Remove excessive whitespace
        return text.replaceAll("\\s+", " ")
                   .replaceAll(" +", " ")
                   .trim();
    }

    private List<String> extractSections(String content) {
        // Simple section extraction based on newlines
        List<String> sections = new ArrayList<>();
        String[] paragraphs = content.split("\n\n+");
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty() && trimmed.length() > 50) {
                sections.add(trimmed.substring(0, Math.min(100, trimmed.length())) + "...");
            }
        }
        return sections;
    }

    private long countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.split("\\s+").length;
    }
}
