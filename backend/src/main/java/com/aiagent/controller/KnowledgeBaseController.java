package com.aiagent.controller;

import com.aiagent.dto.knowledge.*;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.DocumentService;
import com.aiagent.service.KnowledgeBaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<KnowledgeBaseResponse>> getAllKnowledgeBases(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(knowledgeBaseService.getAllKnowledgeBases(principal.getId()));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<KnowledgeBaseResponse>> getKnowledgeBasesPaged(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(knowledgeBaseService.getKnowledgeBasesPaged(principal.getId(), pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<KnowledgeBaseResponse>> getActiveKnowledgeBases(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(knowledgeBaseService.getActiveKnowledgeBases(principal.getId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<KnowledgeBaseResponse>> searchKnowledgeBases(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query) {
        return ResponseEntity.ok(knowledgeBaseService.searchKnowledgeBases(principal.getId(), query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBaseResponse> getKnowledgeBase(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(knowledgeBaseService.getKnowledgeBase(id, principal.getId()));
    }

    @PostMapping
    public ResponseEntity<KnowledgeBaseResponse> createKnowledgeBase(
            @Valid @RequestBody KnowledgeBaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBaseResponse> updateKnowledgeBase(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(knowledgeBaseService.updateKnowledgeBase(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteKnowledgeBase(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        knowledgeBaseService.deleteKnowledgeBase(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<KnowledgeBaseResponse> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(knowledgeBaseService.toggleActive(id, principal.getId()));
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<Map<String, String>> reindex(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        knowledgeBaseService.reindex(id, principal.getId());
        return ResponseEntity.ok(Map.of("message", "Reindexing started"));
    }

    @PostMapping("/{id}/search")
    public ResponseEntity<SearchResult> search(
            @PathVariable Long id,
            @Valid @RequestBody SearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(knowledgeBaseService.search(id, principal.getId(), request));
    }

    // Document endpoints
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<DocumentResponse>> getDocuments(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDocuments(id, principal.getId()));
    }

    @GetMapping("/{id}/documents/{docId}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @PathVariable Long docId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.getDocument(id, docId, principal.getId()));
    }

    @PostMapping(value = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        DocumentResponse response = documentService.uploadDocument(id, principal.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @PathVariable Long docId,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        documentService.deleteDocument(id, docId, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/documents/{docId}/retry")
    public ResponseEntity<DocumentResponse> retryDocumentProcessing(
            @PathVariable Long id,
            @PathVariable Long docId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(documentService.retryProcessing(id, docId, principal.getId()));
    }

    @GetMapping("/{id}/documents/{docId}/progress")
    public ResponseEntity<Map<String, Object>> getDocumentProgress(
            @PathVariable Long id,
            @PathVariable Long docId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DocumentResponse doc = documentService.getDocument(id, docId, principal.getId());
        return ResponseEntity.ok(Map.of(
                "documentId", doc.getId(),
                "fileName", doc.getFileName(),
                "status", doc.getStatus(),
                "progress", doc.getProgress(),
                "processingStage", doc.getProcessingStage() != null ? doc.getProcessingStage() : "",
                "chunkCount", doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                "errorMessage", doc.getErrorMessage() != null ? doc.getErrorMessage() : ""
        ));
    }
}
