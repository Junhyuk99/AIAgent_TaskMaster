package com.aiagent.parser;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class DocumentParserFactory {

    private final Map<String, DocumentParser> parsersByExtension = new HashMap<>();
    private final Map<String, DocumentParser> parsersByMimeType = new HashMap<>();

    public DocumentParserFactory(List<DocumentParser> parsers) {
        for (DocumentParser parser : parsers) {
            for (String ext : parser.getSupportedExtensions()) {
                parsersByExtension.put(ext.toLowerCase(), parser);
            }
            for (String mimeType : parser.getSupportedMimeTypes()) {
                parsersByMimeType.put(mimeType.toLowerCase(), parser);
            }
        }
        log.info("DocumentParserFactory initialized with {} parsers supporting {} extensions",
                parsers.size(), parsersByExtension.size());
    }

    public DocumentParser getParser(String fileName) {
        String extension = getExtension(fileName);
        DocumentParser parser = parsersByExtension.get(extension.toLowerCase());
        if (parser == null) {
            throw new UnsupportedFileTypeException(
                    String.format("Unsupported file type: %s (extension: %s)", fileName, extension)
            );
        }
        return parser;
    }

    public DocumentParser getParserByMimeType(String mimeType) {
        if (mimeType == null) {
            throw new UnsupportedFileTypeException("MIME type is null");
        }
        DocumentParser parser = parsersByMimeType.get(mimeType.toLowerCase());
        if (parser == null) {
            throw new UnsupportedFileTypeException(
                    String.format("Unsupported MIME type: %s", mimeType)
            );
        }
        return parser;
    }

    public boolean isSupported(String fileName) {
        String extension = getExtension(fileName);
        return parsersByExtension.containsKey(extension.toLowerCase());
    }

    public boolean isSupportedMimeType(String mimeType) {
        return mimeType != null && parsersByMimeType.containsKey(mimeType.toLowerCase());
    }

    private String getExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1);
    }
}
