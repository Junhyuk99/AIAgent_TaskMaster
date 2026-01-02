package com.aiagent.repository;

import com.aiagent.entity.Agent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    List<Agent> findByUserId(Long userId);
    Page<Agent> findByUserId(Long userId, Pageable pageable);
    List<Agent> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT a FROM Agent a LEFT JOIN FETCH a.llmServer WHERE a.id = :id")
    Optional<Agent> findByIdWithLlmServer(@Param("id") Long id);

    @Query("SELECT a FROM Agent a LEFT JOIN FETCH a.functions WHERE a.id = :id")
    Optional<Agent> findByIdWithFunctions(@Param("id") Long id);

    @Query("SELECT a FROM Agent a LEFT JOIN FETCH a.knowledgeBases WHERE a.id = :id")
    Optional<Agent> findByIdWithKnowledgeBases(@Param("id") Long id);

    @Query("SELECT DISTINCT a FROM Agent a " +
           "LEFT JOIN FETCH a.llmServer " +
           "LEFT JOIN FETCH a.knowledgeBases " +
           "LEFT JOIN FETCH a.functions " +
           "WHERE a.id = :id")
    Optional<Agent> findByIdWithAllRelations(@Param("id") Long id);

    Optional<Agent> findBySlug(String slug);

    @Query("SELECT DISTINCT a FROM Agent a " +
           "LEFT JOIN FETCH a.llmServer " +
           "LEFT JOIN FETCH a.knowledgeBases " +
           "LEFT JOIN FETCH a.functions " +
           "WHERE a.slug = :slug AND a.isActive = true")
    Optional<Agent> findBySlugWithAllRelations(@Param("slug") String slug);

    boolean existsBySlug(String slug);
}
