package com.aiagent.repository;

import com.aiagent.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt ASC")
    Page<Message> findByConversationIdPaged(@Param("conversationId") Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversation.externalId = :externalId ORDER BY m.createdAt ASC")
    List<Message> findByConversationExternalId(@Param("externalId") String externalId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.id = :conversationId")
    long countByConversationId(@Param("conversationId") Long conversationId);

    @Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT :limit")
    List<Message> findRecentByConversationId(@Param("conversationId") Long conversationId, @Param("limit") int limit);

    // Statistics queries
    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.user.id = :userId AND m.createdAt >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(m.tokenCount), 0) FROM Message m WHERE m.conversation.user.id = :userId AND m.createdAt >= :since")
    Long sumTokensByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(m.tokenCount), 0) FROM Message m WHERE m.conversation.agent.id = :agentId AND m.createdAt >= :since")
    Long sumTokensByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.conversation.agent.id = :agentId AND m.createdAt >= :since")
    long countByAgentIdSince(@Param("agentId") Long agentId, @Param("since") LocalDateTime since);

    @Query(value = "SELECT DATE(m.createdAt) as date, COUNT(m) as count, COALESCE(SUM(m.tokenCount), 0) as tokens FROM Message m " +
           "WHERE m.conversation.user.id = :userId AND m.createdAt >= :since " +
           "GROUP BY DATE(m.createdAt) ORDER BY date")
    List<Object[]> getDailyStatsByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT m.modelUsed, COUNT(m), COALESCE(SUM(m.tokenCount), 0) FROM Message m " +
           "WHERE m.conversation.user.id = :userId AND m.createdAt >= :since AND m.modelUsed IS NOT NULL AND m.role = 'ASSISTANT' " +
           "GROUP BY m.modelUsed ORDER BY COUNT(m) DESC")
    List<Object[]> getModelStatsByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
