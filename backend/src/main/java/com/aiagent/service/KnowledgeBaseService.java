package com.aiagent.service;

import com.aiagent.dto.knowledge.KnowledgeBaseRequest;
import com.aiagent.dto.knowledge.KnowledgeBaseResponse;
import com.aiagent.dto.knowledge.SearchRequest;
import com.aiagent.dto.knowledge.SearchResult;
import com.aiagent.embedding.EmbeddingService;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.entity.User;
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

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final UserRepository userRepository;
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

        // Generate embedding for query
        float[] queryEmbedding = embeddingService.embed(request.getQuery());

        // Search vector store
        int topK = request.getTopK() != null ? request.getTopK() : 5;
        List<VectorSearchResult> vectorResults = vectorStore.search(
                kb.getCollectionName(),
                queryEmbedding,
                topK,
                request.getMetadataFilter()
        );

        // Convert to SearchResult matches
        List<SearchResult.SearchMatch> matches = vectorResults.stream()
                .map(vr -> SearchResult.SearchMatch.builder()
                        .chunkId(vr.getId())
                        .content(vr.getContent())
                        .score(vr.getScore())
                        .metadata(vr.getMetadata())
                        .build())
                .toList();

        log.debug("Vector search for query '{}' returned {} results", request.getQuery(), matches.size());

        return SearchResult.builder()
                .query(request.getQuery())
                .matches(matches)
                .searchTimeMs(System.currentTimeMillis() - startTime)
                .build();
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
