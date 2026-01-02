package com.aiagent.vectorstore;

import java.util.List;
import java.util.Map;

public interface VectorStore {

    void createCollection(String collectionName, int dimension);

    void deleteCollection(String collectionName);

    boolean collectionExists(String collectionName);

    void upsert(String collectionName, List<VectorDocument> documents);

    void deleteByDocumentId(String collectionName, String documentId);

    List<VectorSearchResult> search(String collectionName, float[] queryVector, int topK, Map<String, Object> metadataFilter);

    long getDocumentCount(String collectionName);

    static String buildCollectionName(Long knowledgeBaseId, Long userId) {
        return "kb_" + knowledgeBaseId + "_" + userId;
    }
}
