package com.aiagent.service;

import com.aiagent.dto.knowledge.KnowledgeBaseRequest;
import com.aiagent.dto.knowledge.KnowledgeBaseResponse;
import com.aiagent.dto.knowledge.SearchRequest;
import com.aiagent.dto.knowledge.SearchResult;
import com.aiagent.embedding.EmbeddingService;
import com.aiagent.entity.DocumentChunk;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.entity.User;
import com.aiagent.repository.DocumentChunkRepository;
import com.aiagent.repository.KnowledgeBaseRepository;
import com.aiagent.repository.UserRepository;
import com.aiagent.vectorstore.VectorSearchResult;
import com.aiagent.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private static final double VECTOR_WEIGHT = 0.7;
    private static final double KEYWORD_WEIGHT = 0.3;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final UserRepository userRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final VectorStore vectorStore;
    private final EmbeddingService embeddingService;

    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> getAllKnowledgeBases(Long userId) {
        return knowledgeBaseRepository.findByUserId(userId).stream()
                .map(KnowledgeBaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeBaseResponse> getKnowledgeBasesPaged(Long userId, Pageable pageable) {
        return knowledgeBaseRepository.findByUserId(userId, pageable)
                .map(KnowledgeBaseResponse::from);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> getActiveKnowledgeBases(Long userId) {
        return knowledgeBaseRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(KnowledgeBaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseResponse getKnowledgeBase(Long id, Long userId) {
        KnowledgeBase kb = knowledgeBaseRepository.findByIdWithDocuments(id)
                .filter(k -> k.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Knowledge Base not found or access denied"));
        return KnowledgeBaseResponse.fromWithDocuments(kb);
    }

    @Transactional
    public KnowledgeBaseResponse createKnowledgeBase(KnowledgeBaseRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        KnowledgeBase kb = KnowledgeBase.builder()
                .name(request.getName())
                .description(request.getDescription())
                .chunkSize(request.getChunkSize())
                .chunkOverlap(request.getChunkOverlap())
                .chunkingStrategy(request.getChunkingStrategy())
                .isActive(true)
                .user(user)
                .build();

        KnowledgeBase saved = knowledgeBaseRepository.save(kb);

        // Create vector collection with proper naming
        String collectionName = VectorStore.buildCollectionName(saved.getId(), userId);
        saved.setCollectionName(collectionName);
        saved = knowledgeBaseRepository.save(saved);

        try {
            int dimension = embeddingService.getDimension();
            vectorStore.createCollection(collectionName, dimension);
            log.info("Created vector collection: {} with dimension: {}", collectionName, dimension);
        } catch (Exception e) {
            log.error("Failed to create vector collection: {}", e.getMessage());
            // Don't fail the KB creation, but log the error
        }

        log.info("Created knowledge base: {} for user: {}", saved.getName(), userId);
        return KnowledgeBaseResponse.from(saved);
    }

    @Transactional
    public KnowledgeBaseResponse updateKnowledgeBase(Long id, KnowledgeBaseRequest request, Long userId) {
        KnowledgeBase kb = findKnowledgeBaseByIdAndUser(id, userId);

        kb.setName(request.getName());
        kb.setDescription(request.getDescription());
        kb.setChunkSize(request.getChunkSize());
        kb.setChunkOverlap(request.getChunkOverlap());
        kb.setChunkingStrategy(request.getChunkingStrategy());

        KnowledgeBase updated = knowledgeBaseRepository.save(kb);
        log.info("Updated knowledge base: {}", updated.getName());
        return KnowledgeBaseResponse.from(updated);
    }

    @Transactional
    public void deleteKnowledgeBase(Long id, Long userId) {
        KnowledgeBase kb = findKnowledgeBaseByIdAndUser(id, userId);

        // Delete vector collection if exists
        if (kb.getCollectionName() != null) {
            try {
                vectorStore.deleteCollection(kb.getCollectionName());
                log.info("Deleted vector collection: {}", kb.getCollectionName());
            } catch (Exception e) {
                log.error("Failed to delete vector collection: {}", e.getMessage());
            }
        }

        knowledgeBaseRepository.delete(kb);
        log.info("Deleted knowledge base: {}", kb.getName());
    }

    @Transactional
    public KnowledgeBaseResponse toggleActive(Long id, Long userId) {
        KnowledgeBase kb = findKnowledgeBaseByIdAndUser(id, userId);
        kb.setIsActive(!kb.getIsActive());
        KnowledgeBase updated = knowledgeBaseRepository.save(kb);
        log.info("Toggled knowledge base active status: {} -> {}", kb.getName(), updated.getIsActive());
        return KnowledgeBaseResponse.from(updated);
    }

    public void reindex(Long id, Long userId) {
        KnowledgeBase kb = findKnowledgeBaseByIdAndUser(id, userId);
        // TODO: Implement actual reindexing with vector database
        log.info("Reindex requested for knowledge base: {}", kb.getName());
    }

    public SearchResult search(Long id, Long userId, SearchRequest request) {
        KnowledgeBase kb = findKnowledgeBaseByIdAndUser(id, userId);
        long startTime = System.currentTimeMillis();

        if (kb.getCollectionName() == null) {
            return SearchResult.builder()
                    .query(request.getQuery())
                    .matches(List.of())
                    .searchTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        int topK = request.getTopK() != null ? request.getTopK() : 5;

        // 1. Vector search (semantic)
        float[] queryEmbedding = embeddingService.embed(request.getQuery());
        List<VectorSearchResult> vectorResults = vectorStore.search(
                kb.getCollectionName(),
                queryEmbedding,
                topK * 2,  // Get more results for hybrid merge
                request.getMetadataFilter()
        );

        // 2. Keyword search (PostgreSQL FTS)
        List<DocumentChunk> keywordResults = documentChunkRepository.searchByKeyword(
                kb.getId(),
                request.getQuery(),
                topK * 2
        );

        // If FTS returns nothing, try ILIKE fallback
        if (keywordResults.isEmpty()) {
            keywordResults = documentChunkRepository.searchByContentLike(
                    kb.getId(),
                    request.getQuery(),
                    topK * 2
            );
        }

        // 3. Hybrid merge with weighted scoring
        Map<String, HybridResult> mergedResults = new HashMap<>();

        // Add vector results
        for (int i = 0; i < vectorResults.size(); i++) {
            VectorSearchResult vr = vectorResults.get(i);
            double vectorScore = vr.getScore() * VECTOR_WEIGHT;
            mergedResults.put(vr.getId(), new HybridResult(
                    vr.getId(),
                    vr.getContent(),
                    vectorScore,
                    vr.getMetadata(),
                    true,
                    false
            ));
        }

        // Add/merge keyword results
        for (int i = 0; i < keywordResults.size(); i++) {
            DocumentChunk chunk = keywordResults.get(i);
            // Normalize keyword rank to score (higher rank = lower score)
            double keywordScore = (1.0 - (i / (double) keywordResults.size())) * KEYWORD_WEIGHT;

            HybridResult existing = mergedResults.get(chunk.getChunkId());
            if (existing != null) {
                // Boost score if found by both methods
                existing.score += keywordScore;
                existing.keywordMatch = true;
            } else {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("documentId", chunk.getDocumentId().toString());
                metadata.put("documentName", chunk.getDocumentName());
                metadata.put("chunkIndex", chunk.getChunkIndex());

                mergedResults.put(chunk.getChunkId(), new HybridResult(
                        chunk.getChunkId(),
                        chunk.getContent(),
                        keywordScore,
                        metadata,
                        false,
                        true
                ));
            }
        }

        // Sort by combined score and take top K
        List<SearchResult.SearchMatch> matches = mergedResults.values().stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(topK)
                .map(hr -> SearchResult.SearchMatch.builder()
                        .chunkId(hr.chunkId)
                        .content(hr.content)
                        .score(hr.score)
                        .metadata(hr.metadata)
                        .build())
                .toList();

        log.debug("Hybrid search for query '{}': {} vector, {} keyword, {} merged results",
                request.getQuery(), vectorResults.size(), keywordResults.size(), matches.size());

        return SearchResult.builder()
                .query(request.getQuery())
                .matches(matches)
                .searchTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private static class HybridResult {
        String chunkId;
        String content;
        double score;
        Map<String, Object> metadata;
        boolean vectorMatch;
        boolean keywordMatch;

        HybridResult(String chunkId, String content, double score,
                     Map<String, Object> metadata, boolean vectorMatch, boolean keywordMatch) {
            this.chunkId = chunkId;
            this.content = content;
            this.score = score;
            this.metadata = metadata;
            this.vectorMatch = vectorMatch;
            this.keywordMatch = keywordMatch;
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeBaseResponse> searchKnowledgeBases(Long userId, String query) {
        return knowledgeBaseRepository.findByUserId(userId).stream()
                .filter(kb -> kb.getName().toLowerCase().contains(query.toLowerCase()) ||
                        (kb.getDescription() != null &&
                         kb.getDescription().toLowerCase().contains(query.toLowerCase())))
                .map(KnowledgeBaseResponse::from)
                .toList();
    }

    private KnowledgeBase findKnowledgeBaseByIdAndUser(Long id, Long userId) {
        return knowledgeBaseRepository.findById(id)
                .filter(kb -> kb.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Knowledge Base not found or access denied"));
    }
}
