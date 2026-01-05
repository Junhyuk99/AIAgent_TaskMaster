package com.aiagent.repository;

import com.aiagent.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    List<DocumentChunk> findByKnowledgeBaseId(Long knowledgeBaseId);

    void deleteByDocumentId(Long documentId);

    void deleteByKnowledgeBaseId(Long knowledgeBaseId);

    /**
     * PostgreSQL full-text search using plainto_tsquery for keyword matching.
     * Returns chunks that match the search query, ordered by relevance.
     */
    @Query(value = """
        SELECT dc.* FROM document_chunks dc
        WHERE dc.knowledge_base_id = :knowledgeBaseId
        AND to_tsvector('simple', dc.content) @@ plainto_tsquery('simple', :query)
        ORDER BY ts_rank(to_tsvector('simple', dc.content), plainto_tsquery('simple', :query)) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> searchByKeyword(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("query") String query,
            @Param("limit") int limit);

    /**
     * Simple ILIKE search as fallback for short queries or when FTS doesn't match.
     */
    @Query(value = """
        SELECT dc.* FROM document_chunks dc
        WHERE dc.knowledge_base_id = :knowledgeBaseId
        AND dc.content ILIKE CONCAT('%', :query, '%')
        LIMIT :limit
        """, nativeQuery = true)
    List<DocumentChunk> searchByContentLike(
            @Param("knowledgeBaseId") Long knowledgeBaseId,
            @Param("query") String query,
            @Param("limit") int limit);
}
