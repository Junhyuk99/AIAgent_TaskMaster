package com.aiagent.service;

import com.aiagent.dto.knowledge.DocumentResponse;
import com.aiagent.entity.Document;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.repository.DocumentRepository;
import com.aiagent.repository.KnowledgeBaseRepository;
import com.aiagent.service.storage.FileStorageService;
import com.aiagent.util.FileValidationUtil;
import com.aiagent.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final FileStorageService fileStorageService;
    private final DocumentProcessingService documentProcessingService;
    private final VectorStore vectorStore;

    @Transactional(readOnly = true)
    public List<DocumentResponse> getDocuments(Long knowledgeBaseId, Long userId) {
        validateKnowledgeBaseAccess(knowledgeBaseId, userId);
        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocument(Long knowledgeBaseId, Long documentId, Long userId) {
        validateKnowledgeBaseAccess(knowledgeBaseId, userId);
        Document document = documentRepository.findById(documentId)
                .filter(doc -> doc.getKnowledgeBase().getId().equals(knowledgeBaseId))
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return DocumentResponse.from(document);
    }

    @Transactional
    public DocumentResponse uploadDocument(Long knowledgeBaseId, Long userId, MultipartFile file) throws IOException {
        // Validate file
        FileValidationUtil.ValidationResult validationResult = FileValidationUtil.validate(file);
        if (!validationResult.valid()) {
            throw new IllegalArgumentException(validationResult.errorMessage());
        }

        KnowledgeBase kb = validateKnowledgeBaseAccess(knowledgeBaseId, userId);

        String originalFileName = file.getOriginalFilename();
        String directory = "kb_" + knowledgeBaseId;

        String storedPath = fileStorageService.store(file, directory);

        Document document = Document.builder()
                .fileName(originalFileName)
                .filePath(storedPath)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .status(Document.DocumentStatus.PENDING)
                .chunkCount(0)
                .progress(0)
                .processingStage("Uploaded")
                .knowledgeBase(kb)
                .build();

        Document saved = documentRepository.save(document);
        log.info("Uploaded document: {} to knowledge base: {}", originalFileName, kb.getName());

        // TODO: Trigger async document processing
        processDocumentAsync(saved.getId());

        return DocumentResponse.from(saved);
    }

    @Transactional
    public void deleteDocument(Long knowledgeBaseId, Long documentId, Long userId) throws IOException {
        KnowledgeBase kb = validateKnowledgeBaseAccess(knowledgeBaseId, userId);

        Document document = documentRepository.findById(documentId)
                .filter(doc -> doc.getKnowledgeBase().getId().equals(knowledgeBaseId))
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Delete vectors from vector store
        if (kb.getCollectionName() != null) {
            try {
                vectorStore.deleteByDocumentId(kb.getCollectionName(), documentId.toString());
                log.info("Deleted vectors for document: {} from collection: {}", documentId, kb.getCollectionName());
            } catch (Exception e) {
                log.error("Failed to delete vectors for document: {}", documentId, e);
            }
        }

        fileStorageService.delete(document.getFilePath());

        documentRepository.delete(document);
        log.info("Deleted document: {}", document.getFileName());
    }

    @Transactional
    public DocumentResponse retryProcessing(Long knowledgeBaseId, Long documentId, Long userId) {
        KnowledgeBase kb = validateKnowledgeBaseAccess(knowledgeBaseId, userId);

        Document document = documentRepository.findById(documentId)
                .filter(doc -> doc.getKnowledgeBase().getId().equals(knowledgeBaseId))
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Delete existing vectors before reprocessing
        if (kb.getCollectionName() != null) {
            try {
                vectorStore.deleteByDocumentId(kb.getCollectionName(), documentId.toString());
                log.info("Deleted existing vectors for document: {} before retry", documentId);
            } catch (Exception e) {
                log.warn("Failed to delete existing vectors: {}", e.getMessage());
            }
        }

        document.setStatus(Document.DocumentStatus.PENDING);
        document.setProgress(0);
        document.setErrorMessage(null);
        document.setProcessingStage("Queued for retry");

        Document saved = documentRepository.save(document);

        processDocumentAsync(saved.getId());

        return DocumentResponse.from(saved);
    }

    private void processDocumentAsync(Long documentId) {
        documentProcessingService.processDocument(documentId);
        log.info("Document processing started for document ID: {}", documentId);
    }

    private KnowledgeBase validateKnowledgeBaseAccess(Long knowledgeBaseId, Long userId) {
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .filter(kb -> kb.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Knowledge Base not found or access denied"));
    }
}
