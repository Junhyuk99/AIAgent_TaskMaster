package com.aiagent.service;

import com.aiagent.dto.rag.RagContext;
import com.aiagent.dto.rag.RagSearchRequest;
import com.aiagent.dto.rag.RagSearchResponse;
import com.aiagent.embedding.EmbeddingService;
import com.aiagent.entity.Document;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.repository.DocumentRepository;
import com.aiagent.vectorstore.VectorSearchResult;
import com.aiagent.vectorstore.VectorStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagService {

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;

    @Value("${rag.default-top-k:5}")
    private int defaultTopK;

    @Value("${rag.default-min-score:0.5}")
    private double defaultMinScore;

    @Value("${rag.max-context-length:4000}")
    private int maxContextLength;

    private static final String RAG_PROMPT_TEMPLATE = """

        The following are relevant documents that may help answer the user's question:

        %s

        Please use the information from these documents to provide an accurate and helpful response.
        If the documents don't contain relevant information, you may use your general knowledge but mention this to the user.
        When citing information, reference the source document when possible.
        """;

    /**
     * Search for relevant chunks across multiple knowledge bases
     */
    public RagSearchResponse searchRelevantChunks(String query, Set<KnowledgeBase> knowledgeBases, Integer topK, Double minScore) {
        long startTime = System.currentTimeMillis();

        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return RagSearchResponse.builder()
                    .formattedContext("")
                    .sources(List.of())
                    .retrievedChunks(List.of())
                    .searchTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        int effectiveTopK = topK != null ? topK : defaultTopK;
        double effectiveMinScore = minScore != null ? minScore : defaultMinScore;

        // Generate query embedding
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingService.embed(query);
        } catch (Exception e) {
            log.error("Failed to generate query embedding: {}", e.getMessage());
            return RagSearchResponse.builder()
                    .formattedContext("")
                    .sources(List.of())
                    .retrievedChunks(List.of())
                    .searchTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }

        // Search across all knowledge bases
        List<RagSearchResponse.RetrievedChunk> allChunks = new ArrayList<>();

        for (KnowledgeBase kb : knowledgeBases) {
            if (!kb.getIsActive() || kb.getCollectionName() == null) {
                log.debug("Skipping KB {} (active={}, collection={})",
                        kb.getName(), kb.getIsActive(), kb.getCollectionName());
                continue;
            }

            log.info("Searching knowledge base '{}' (id={}) with collection '{}'",
                    kb.getName(), kb.getId(), kb.getCollectionName());

            try {
                List<VectorSearchResult> results = vectorStore.search(
                        kb.getCollectionName(),
                        queryEmbedding,
                        effectiveTopK,
                        null // No metadata filter for now
                );

                log.info("KB '{}' returned {} results", kb.getName(), results.size());

                for (VectorSearchResult result : results) {
                    if (result.getScore() >= effectiveMinScore) {
                        // Extract metadata
                        String documentId = extractStringFromMetadata(result.getMetadata(), "documentId");
                        String documentName = extractStringFromMetadata(result.getMetadata(), "documentName");
                        Integer chunkIndex = extractIntFromMetadata(result.getMetadata(), "chunkIndex");

                        // If document name not in metadata, try to fetch from database
                        if (documentName == null && documentId != null) {
                            documentName = getDocumentName(documentId);
                        }

                        allChunks.add(RagSearchResponse.RetrievedChunk.builder()
                                .chunkId(result.getId())
                                .content(result.getContent())
                                .score(result.getScore())
                                .documentId(documentId)
                                .documentName(documentName)
                                .chunkIndex(chunkIndex)
                                .build());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to search knowledge base {}: {}", kb.getName(), e.getMessage());
            }
        }

        // Sort by score descending and take top K
        allChunks.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        if (allChunks.size() > effectiveTopK) {
            allChunks = allChunks.subList(0, effectiveTopK);
        }

        // Build sources list (deduplicated by document)
        List<RagSearchResponse.DocumentSource> sources = buildSourcesList(allChunks);

        // Format context
        String formattedContext = formatContext(allChunks);

        log.debug("RAG search found {} chunks from {} knowledge bases in {}ms",
                allChunks.size(), knowledgeBases.size(), System.currentTimeMillis() - startTime);

        return RagSearchResponse.builder()
                .formattedContext(formattedContext)
                .sources(sources)
                .retrievedChunks(allChunks)
                .searchTimeMs(System.currentTimeMillis() - startTime)
                .build();
    }

    /**
     * Build augmented system prompt with RAG context
     */
    public RagContext buildRagContext(String originalSystemPrompt, String query, Set<KnowledgeBase> knowledgeBases) {
        RagSearchResponse searchResponse = searchRelevantChunks(query, knowledgeBases, null, null);

        if (searchResponse.getRetrievedChunks().isEmpty()) {
            return RagContext.builder()
                    .originalSystemPrompt(originalSystemPrompt)
                    .augmentedSystemPrompt(originalSystemPrompt)
                    .retrievedContext("")
                    .sources(List.of())
                    .contextFound(false)
                    .build();
        }

        String ragPromptAddition = String.format(RAG_PROMPT_TEMPLATE, searchResponse.getFormattedContext());
        String augmentedPrompt = originalSystemPrompt + ragPromptAddition;

        return RagContext.builder()
                .originalSystemPrompt(originalSystemPrompt)
                .augmentedSystemPrompt(augmentedPrompt)
                .retrievedContext(searchResponse.getFormattedContext())
                .sources(searchResponse.getSources())
                .contextFound(true)
                .build();
    }

    /**
     * Format retrieved chunks into a readable context string
     */
    private String formatContext(List<RagSearchResponse.RetrievedChunk> chunks) {
        if (chunks.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int currentLength = 0;
        int docIndex = 1;

        for (RagSearchResponse.RetrievedChunk chunk : chunks) {
            String docHeader = String.format("[Document %d: %s]%n",
                    docIndex,
                    chunk.getDocumentName() != null ? chunk.getDocumentName() : "Unknown");

            String content = chunk.getContent();

            // Check if we exceed max length
            if (currentLength + docHeader.length() + content.length() > maxContextLength) {
                // Truncate content to fit
                int remaining = maxContextLength - currentLength - docHeader.length() - 50; // 50 for ellipsis etc
                if (remaining > 100) {
                    sb.append(docHeader);
                    sb.append(content.substring(0, remaining));
                    sb.append("...\n\n");
                }
                break;
            }

            sb.append(docHeader);
            sb.append(content);
            sb.append("\n\n");

            currentLength += docHeader.length() + content.length() + 2;
            docIndex++;
        }

        return sb.toString().trim();
    }

    /**
     * Build deduplicated sources list
     */
    private List<RagSearchResponse.DocumentSource> buildSourcesList(List<RagSearchResponse.RetrievedChunk> chunks) {
        Map<String, RagSearchResponse.DocumentSource> sourceMap = new LinkedHashMap<>();

        for (RagSearchResponse.RetrievedChunk chunk : chunks) {
            String key = chunk.getDocumentId() != null ? chunk.getDocumentId() : chunk.getChunkId();

            // Keep the highest scoring chunk for each document
            if (!sourceMap.containsKey(key) ||
                    sourceMap.get(key).getScore() < chunk.getScore()) {
                sourceMap.put(key, RagSearchResponse.DocumentSource.builder()
                        .documentId(chunk.getDocumentId())
                        .documentName(chunk.getDocumentName())
                        .chunkIndex(chunk.getChunkIndex())
                        .score(chunk.getScore())
                        .build());
            }
        }

        return new ArrayList<>(sourceMap.values());
    }

    private String extractStringFromMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer extractIntFromMetadata(Map<String, Object> metadata, String key) {
        if (metadata == null || !metadata.containsKey(key)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String getDocumentName(String documentId) {
        try {
            Long id = Long.parseLong(documentId);
            return documentRepository.findById(id)
                    .map(Document::getFileName)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
