package com.aiagent.repository;

import com.aiagent.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByExternalId(String externalId);

    @Query("SELECT c FROM Conversation c WHERE c.externalId = :externalId AND c.user.id = :userId")
    Optional<Conversation> findByExternalIdAndUserId(@Param("externalId") String externalId, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.agent.id = :agentId AND c.user.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByAgentIdAndUserId(@Param("agentId") Long agentId, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.agent.id = :agentId AND c.user.id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByAgentIdAndUserIdPaged(@Param("agentId") Long agentId, @Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.user.id = :userId ORDER BY c.updatedAt DESC")
    Page<Conversation> findByUserIdPaged(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.messages WHERE c.id = :id")
    Optional<Conversation> findByIdWithMessages(@Param("id") Long id);

    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.messages WHERE c.externalId = :externalId AND c.user.id = :userId")
    Optional<Conversation> findByExternalIdWithMessages(@Param("externalId") String externalId, @Param("userId") Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.agent.id = :agentId AND c.user.id = :userId AND c.isArchived = false ORDER BY c.updatedAt DESC")
    List<Conversation> findActiveByAgentIdAndUserId(@Param("agentId") Long agentId, @Param("userId") Long userId);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.agent.id = :agentId AND c.user.id = :userId")
    long countByAgentIdAndUserId(@Param("agentId") Long agentId, @Param("userId") Long userId);

    // Statistics queries
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.user.id = :userId AND c.createdAt >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.agent.id = :agentId AND c.createdAt >= :since")
    long countByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE(c.createdAt) as date, COUNT(c) as count FROM Conversation c " +
           "WHERE c.user.id = :userId AND c.createdAt >= :since " +
           "GROUP BY DATE(c.createdAt) ORDER BY date")
    List<Object[]> countDailyByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT c.agent.id, c.agent.name, COUNT(c) FROM Conversation c " +
           "WHERE c.user.id = :userId AND c.createdAt >= :since " +
           "GROUP BY c.agent.id, c.agent.name ORDER BY COUNT(c) DESC")
    List<Object[]> countByAgentForUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Modifying
    @Query(value = "DELETE FROM conversations WHERE agent_id = :agentId", nativeQuery = true)
    void deleteByAgentId(@Param("agentId") Long agentId);
}
