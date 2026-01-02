package com.aiagent.repository;

import com.aiagent.entity.KnowledgeBase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByUserId(Long userId);
    Page<KnowledgeBase> findByUserId(Long userId, Pageable pageable);
    List<KnowledgeBase> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT kb FROM KnowledgeBase kb LEFT JOIN FETCH kb.documents WHERE kb.id = :id")
    Optional<KnowledgeBase> findByIdWithDocuments(@Param("id") Long id);
}
