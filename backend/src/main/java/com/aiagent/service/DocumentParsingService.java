package com.aiagent.service;

import com.aiagent.parser.DocumentParser;
import com.aiagent.parser.DocumentParserFactory;
import com.aiagent.parser.ParseResult;
import com.aiagent.service.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParsingService {

    private final DocumentParserFactory parserFactory;
    private final FileStorageService fileStorageService;

    public ParseResult parseDocument(String filePath, String fileName) throws Exception {
        log.info("Starting to parse document: {}", fileName);
        long startTime = System.currentTimeMillis();

        try (InputStream inputStream = fileStorageService.loadAsStream(filePath)) {
            DocumentParser parser = parserFactory.getParser(fileName);
            ParseResult result = parser.parse(inputStream, fileName);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Finished parsing document: {} in {}ms", fileName, duration);

            return result;
        }
    }

    @Async("documentParsingExecutor")
    public CompletableFuture<ParseResult> parseDocumentAsync(String filePath, String fileName) {
        log.info("Starting async parsing of document: {}", fileName);
        long startTime = System.currentTimeMillis();

        try (InputStream inputStream = fileStorageService.loadAsStream(filePath)) {
            DocumentParser parser = parserFactory.getParser(fileName);
            ParseResult result = parser.parse(inputStream, fileName);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Finished async parsing document: {} in {}ms", fileName, duration);

            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.error("Failed to parse document: {} - {}", fileName, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean isSupported(String fileName) {
        return parserFactory.isSupported(fileName);
    }
}
