package com.aiagent.service;

import com.aiagent.chunking.ChunkerFactory;
import com.aiagent.chunking.ChunkingConfig;
import com.aiagent.chunking.TextChunk;
import com.aiagent.embedding.EmbeddingService;
import com.aiagent.entity.Document;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.parser.DocumentParserFactory;
import com.aiagent.parser.ParseResult;
import com.aiagent.repository.DocumentRepository;
import com.aiagent.service.storage.FileStorageService;
import com.aiagent.vectorstore.VectorDocument;
import com.aiagent.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessingService {

    private static final int BATCH_SIZE = 10;

    private final DocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final DocumentParserFactory parserFactory;
    private final ChunkerFactory chunkerFactory;
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    @Async("documentParsingExecutor")
    public CompletableFuture<Void> processDocument(Long documentId) {
        log.info("Starting document processing for document ID: {}", documentId);

        try {
            // 1. Load document and KnowledgeBase data within transaction (extracts all needed values)
            DocumentProcessingContext ctx = loadDocumentContext(documentId);

            // Update status to PROCESSING
            updateStatus(documentId, Document.DocumentStatus.PROCESSING, 0, "Initializing");

            // 2. Parse the document
            updateProgress(documentId, 10, "Parsing document");
            ParseResult parseResult;
            try (InputStream inputStream = fileStorageService.loadAsStream(ctx.filePath)) {
                var parser = parserFactory.getParser(ctx.fileName);
                parseResult = parser.parse(inputStream, ctx.fileName);
            }
            updateProgress(documentId, 20, "Document parsed");

            // 3. Create chunking config from knowledge base settings
            ChunkingConfig chunkingConfig = ChunkingConfig.builder()
                    .chunkSize(ctx.chunkSize)
                    .chunkOverlap(ctx.chunkOverlap)
                    .strategyType(ctx.chunkingStrategyType)
                    .build();

            // 4. Chunk the text
            updateProgress(documentId, 30, "Chunking text");
            List<TextChunk> chunks = chunkerFactory.chunk(parseResult.getContent(), chunkingConfig);
            updateProgress(documentId, 40, "Text chunked into " + chunks.size() + " chunks");

            // 5. Generate embeddings and store in vector database
            updateProgress(documentId, 50, "Generating embeddings");

            if (ctx.collectionName == null) {
                throw new RuntimeException("Knowledge base has no vector collection");
            }

            int totalChunks = chunks.size();
            int processedChunks = 0;
            List<VectorDocument> batchDocuments = new ArrayList<>();

            for (TextChunk chunk : chunks) {
                try {
                    float[] embedding = embeddingService.embed(chunk.getContent());

                    // Create vector document with metadata
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("documentId", documentId.toString());
                    metadata.put("documentName", ctx.fileName);
                    metadata.put("chunkIndex", chunk.getIndex());
                    metadata.put("startOffset", chunk.getStartOffset());
                    metadata.put("endOffset", chunk.getEndOffset());
                    if (chunk.getMetadata() != null) {
                        metadata.putAll(chunk.getMetadata());
                    }

                    String vectorId = documentId + "_" + chunk.getIndex();
                    VectorDocument vectorDoc = VectorDocument.builder()
                            .id(vectorId)
                            .content(chunk.getContent())
                            .embedding(embedding)
                            .metadata(metadata)
                            .build();

                    batchDocuments.add(vectorDoc);

                    // Upsert in batches
                    if (batchDocuments.size() >= BATCH_SIZE) {
                        vectorStore.upsert(ctx.collectionName, batchDocuments);
                        batchDocuments.clear();
                    }

                    processedChunks++;
                    int progress = 50 + (int) ((processedChunks * 40.0) / totalChunks);
                    updateProgress(documentId, progress, "Processing chunk " + processedChunks + "/" + totalChunks);
                } catch (Exception e) {
                    log.warn("Failed to process chunk {}: {}", chunk.getIndex(), e.getMessage());
                }
            }

            // Upsert remaining documents
            if (!batchDocuments.isEmpty()) {
                vectorStore.upsert(ctx.collectionName, batchDocuments);
            }

            // 6. Update document as completed
            updateProgress(documentId, 95, "Finalizing");
            completeDocument(documentId, chunks.size());

            log.info("Document processing completed for document ID: {}, {} chunks created",
                    documentId, chunks.size());

            return CompletableFuture.completedFuture(null);

        } catch (Exception e) {
            log.error("Document processing failed for document ID: {}", documentId, e);
            failDocument(documentId, e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Context class to hold all needed data for document processing.
     * This avoids lazy loading issues by extracting values within a transaction.
     */
    private static class DocumentProcessingContext {
        String fileName;
        String filePath;
        int chunkSize;
        int chunkOverlap;
        ChunkingConfig.ChunkingStrategyType chunkingStrategyType;
        String collectionName;
    }

    @Transactional(readOnly = true)
    public DocumentProcessingContext loadDocumentContext(Long documentId) {
        Document document = documentRepository.findByIdWithKnowledgeBase(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        KnowledgeBase kb = document.getKnowledgeBase();

        DocumentProcessingContext ctx = new DocumentProcessingContext();
        ctx.fileName = document.getFileName();
        ctx.filePath = document.getFilePath();
        ctx.chunkSize = kb.getChunkSize();
        ctx.chunkOverlap = kb.getChunkOverlap();
        ctx.chunkingStrategyType = mapChunkingStrategy(kb.getChunkingStrategy());
        ctx.collectionName = kb.getCollectionName();

        return ctx;
    }

    @Transactional
    public Document updateStatus(Long documentId, Document.DocumentStatus status, int progress, String stage) {
        // Use findByIdWithKnowledgeBase to eagerly fetch KnowledgeBase for async processing
        Document document = documentRepository.findByIdWithKnowledgeBase(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        document.setStatus(status);
        document.setProgress(progress);
        document.setProcessingStage(stage);
        return documentRepository.save(document);
    }

    @Transactional
    public void updateProgress(Long documentId, int progress, String stage) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        document.setProgress(progress);
        document.setProcessingStage(stage);
        documentRepository.save(document);
        log.debug("Document {} progress: {}% - {}", documentId, progress, stage);
    }

    @Transactional
    public void completeDocument(Long documentId, int chunkCount) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        document.setStatus(Document.DocumentStatus.COMPLETED);
        document.setProgress(100);
        document.setProcessingStage("Completed");
        document.setChunkCount(chunkCount);
        document.setErrorMessage(null);
        documentRepository.save(document);
    }

    @Transactional
    public void failDocument(Long documentId, String errorMessage) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));
        document.setStatus(Document.DocumentStatus.FAILED);
        document.setProcessingStage("Failed");
        document.setErrorMessage(errorMessage);
        documentRepository.save(document);
    }

    private ChunkingConfig.ChunkingStrategyType mapChunkingStrategy(KnowledgeBase.ChunkingStrategy strategy) {
        return switch (strategy) {
            case FIXED_SIZE -> ChunkingConfig.ChunkingStrategyType.FIXED_SIZE;
            case PARAGRAPH -> ChunkingConfig.ChunkingStrategyType.PARAGRAPH;
            case SEMANTIC -> ChunkingConfig.ChunkingStrategyType.SEMANTIC;
        };
    }
}
