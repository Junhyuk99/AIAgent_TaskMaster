package com.aiagent.repository;

import com.aiagent.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByKnowledgeBaseId(Long knowledgeBaseId);
    List<Document> findByKnowledgeBaseIdAndStatus(Long knowledgeBaseId, Document.DocumentStatus status);
    int countByKnowledgeBaseId(Long knowledgeBaseId);
}
