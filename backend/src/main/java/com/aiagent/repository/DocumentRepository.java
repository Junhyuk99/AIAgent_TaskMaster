package com.aiagent.repository;

import com.aiagent.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByKnowledgeBaseId(Long knowledgeBaseId);
    List<Document> findByKnowledgeBaseIdAndStatus(Long knowledgeBaseId, Document.DocumentStatus status);
    int countByKnowledgeBaseId(Long knowledgeBaseId);

    @Query("SELECT d FROM Document d JOIN FETCH d.knowledgeBase WHERE d.id = :id")
    Optional<Document> findByIdWithKnowledgeBase(@Param("id") Long id);
}
