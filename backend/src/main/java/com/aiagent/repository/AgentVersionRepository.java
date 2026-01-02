package com.aiagent.repository;

import com.aiagent.entity.AgentVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentVersionRepository extends JpaRepository<AgentVersion, Long> {

    List<AgentVersion> findByAgentIdOrderByVersionNumberDesc(Long agentId);

    Page<AgentVersion> findByAgentIdOrderByVersionNumberDesc(Long agentId, Pageable pageable);

    Optional<AgentVersion> findByAgentIdAndVersionNumber(Long agentId, Integer versionNumber);

    Optional<AgentVersion> findByAgentIdAndIsCurrent(Long agentId, Boolean isCurrent);

    @Query("SELECT MAX(v.versionNumber) FROM AgentVersion v WHERE v.agent.id = :agentId")
    Optional<Integer> findMaxVersionNumber(@Param("agentId") Long agentId);

    @Query("SELECT v FROM AgentVersion v WHERE v.agent.id = :agentId AND v.isCurrent = true")
    Optional<AgentVersion> findCurrentVersion(@Param("agentId") Long agentId);

    @Modifying
    @Query("UPDATE AgentVersion v SET v.isCurrent = false WHERE v.agent.id = :agentId")
    void clearCurrentVersion(@Param("agentId") Long agentId);

    long countByAgentId(Long agentId);

    void deleteByAgentId(Long agentId);

    @Query("SELECT v FROM AgentVersion v WHERE v.agent.id = :agentId ORDER BY v.createdAt DESC LIMIT :limit")
    List<AgentVersion> findRecentVersions(@Param("agentId") Long agentId, @Param("limit") int limit);
}
