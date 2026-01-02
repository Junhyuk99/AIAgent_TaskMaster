package com.aiagent.vectorstore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "vectorstore.type", havingValue = "chroma", matchIfMissing = true)
public class ChromaVectorStore implements VectorStore {

    private static final String DEFAULT_TENANT = "default_tenant";
    private static final String DEFAULT_DATABASE = "default_database";
    private static final String API_BASE = "/api/v2/tenants/" + DEFAULT_TENANT + "/databases/" + DEFAULT_DATABASE;

    private final WebClient webClient;
    private final int timeout;
    private final Map<String, String> collectionIdCache = new HashMap<>();

    public ChromaVectorStore(
            @Value("${vectorstore.chroma.url:http://localhost:8000}") String baseUrl,
            @Value("${vectorstore.chroma.timeout:30000}") int timeout) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.timeout = timeout;
        log.info("ChromaVectorStore initialized with URL: {} using API v2", baseUrl);
    }

    /**
     * Get collection UUID by name. Required for v2 API operations.
     */
    private String getCollectionId(String collectionName) {
        // Check cache first
        if (collectionIdCache.containsKey(collectionName)) {
            return collectionIdCache.get(collectionName);
        }

        try {
            Map<String, Object> response = webClient.get()
                    .uri(API_BASE + "/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response != null && response.containsKey("id")) {
                String id = (String) response.get("id");
                collectionIdCache.put(collectionName, id);
                return id;
            }
            throw new VectorStoreException("Collection ID not found for: " + collectionName, null);
        } catch (WebClientResponseException.NotFound e) {
            throw new VectorStoreException("Collection not found: " + collectionName, e);
        } catch (VectorStoreException e) {
            throw e;
        } catch (Exception e) {
            throw new VectorStoreException("Failed to get collection ID: " + collectionName, e);
        }
    }

    @Override
    public void createCollection(String collectionName, int dimension) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("name", collectionName);
            request.put("metadata", Map.of(
                    "dimension", dimension,
                    "hnsw:space", "cosine"
            ));

            webClient.post()
                    .uri(API_BASE + "/collections")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("Created Chroma collection: {}", collectionName);
        } catch (WebClientResponseException.Conflict e) {
            log.debug("Collection {} already exists", collectionName);
        } catch (Exception e) {
            log.error("Failed to create collection: {}", e.getMessage());
            throw new VectorStoreException("Failed to create collection: " + collectionName, e);
        }
    }

    @Override
    public void deleteCollection(String collectionName) {
        try {
            webClient.delete()
                    .uri(API_BASE + "/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            collectionIdCache.remove(collectionName);
            log.info("Deleted Chroma collection: {}", collectionName);
        } catch (WebClientResponseException.NotFound e) {
            collectionIdCache.remove(collectionName);
            log.debug("Collection {} not found, skipping deletion", collectionName);
        } catch (Exception e) {
            log.error("Failed to delete collection: {}", e.getMessage());
            throw new VectorStoreException("Failed to delete collection: " + collectionName, e);
        }
    }

    @Override
    public boolean collectionExists(String collectionName) {
        try {
            webClient.get()
                    .uri(API_BASE + "/collections/{name}", collectionName)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();
            return true;
        } catch (WebClientResponseException.NotFound e) {
            return false;
        } catch (Exception e) {
            log.error("Failed to check collection existence: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void upsert(String collectionName, List<VectorDocument> documents) {
        if (documents.isEmpty()) {
            return;
        }

        try {
            String collectionId = getCollectionId(collectionName);

            List<String> ids = documents.stream()
                    .map(VectorDocument::getId)
                    .toList();
            List<List<Float>> embeddings = documents.stream()
                    .map(doc -> toFloatList(doc.getEmbedding()))
                    .toList();
            List<Map<String, Object>> metadatas = documents.stream()
                    .map(VectorDocument::getMetadata)
                    .toList();
            List<String> contents = documents.stream()
                    .map(VectorDocument::getContent)
                    .toList();

            Map<String, Object> request = new HashMap<>();
            request.put("ids", ids);
            request.put("embeddings", embeddings);
            request.put("metadatas", metadatas);
            request.put("documents", contents);

            webClient.post()
                    .uri(API_BASE + "/collections/{id}/add", collectionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.debug("Upserted {} documents to collection: {}", documents.size(), collectionName);
        } catch (Exception e) {
            log.error("Failed to upsert documents: {}", e.getMessage());
            throw new VectorStoreException("Failed to upsert documents to collection: " + collectionName, e);
        }
    }

    @Override
    public void deleteByDocumentId(String collectionName, String documentId) {
        try {
            String collectionId = getCollectionId(collectionName);

            Map<String, Object> request = new HashMap<>();
            request.put("where", Map.of("documentId", documentId));

            webClient.post()
                    .uri(API_BASE + "/collections/{id}/delete", collectionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.debug("Deleted vectors for document: {} from collection: {}", documentId, collectionName);
        } catch (Exception e) {
            log.error("Failed to delete vectors: {}", e.getMessage());
            throw new VectorStoreException("Failed to delete vectors for document: " + documentId, e);
        }
    }

    @Override
    public List<VectorSearchResult> search(String collectionName, float[] queryVector, int topK, Map<String, Object> metadataFilter) {
        try {
            String collectionId = getCollectionId(collectionName);

            Map<String, Object> request = new HashMap<>();
            request.put("query_embeddings", List.of(toFloatList(queryVector)));
            request.put("n_results", topK);
            request.put("include", List.of("documents", "metadatas", "distances"));

            if (metadataFilter != null && !metadataFilter.isEmpty()) {
                request.put("where", metadataFilter);
            }

            ChromaQueryResponse response = webClient.post()
                    .uri(API_BASE + "/collections/{id}/query", collectionId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(ChromaQueryResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            if (response == null || response.getIds() == null || response.getIds().isEmpty()) {
                return List.of();
            }

            List<VectorSearchResult> results = new ArrayList<>();
            List<String> ids = response.getIds().get(0);
            List<String> documents = response.getDocuments() != null && !response.getDocuments().isEmpty()
                    ? response.getDocuments().get(0) : List.of();
            List<Map<String, Object>> metadatas = response.getMetadatas() != null && !response.getMetadatas().isEmpty()
                    ? response.getMetadatas().get(0) : List.of();
            List<Double> distances = response.getDistances() != null && !response.getDistances().isEmpty()
                    ? response.getDistances().get(0) : List.of();

            for (int i = 0; i < ids.size(); i++) {
                double distance = i < distances.size() ? distances.get(i) : 0.0;
                double score = 1.0 - (distance / 2.0); // Convert distance to similarity score

                results.add(VectorSearchResult.builder()
                        .id(ids.get(i))
                        .content(i < documents.size() ? documents.get(i) : "")
                        .score(score)
                        .metadata(i < metadatas.size() ? metadatas.get(i) : Map.of())
                        .build());
            }

            log.debug("Search returned {} results from collection: {}", results.size(), collectionName);
            return results;
        } catch (Exception e) {
            log.error("Failed to search: {}", e.getMessage());
            throw new VectorStoreException("Failed to search in collection: " + collectionName, e);
        }
    }

    @Override
    public long getDocumentCount(String collectionName) {
        try {
            String collectionId = getCollectionId(collectionName);

            Integer count = webClient.get()
                    .uri(API_BASE + "/collections/{id}/count", collectionId)
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("Failed to get document count: {}", e.getMessage());
            return 0;
        }
    }

    private List<Float> toFloatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }

    @Data
    private static class ChromaQueryResponse {
        private List<List<String>> ids;
        private List<List<String>> documents;
        private List<List<Map<String, Object>>> metadatas;
        private List<List<Double>> distances;
    }
}
