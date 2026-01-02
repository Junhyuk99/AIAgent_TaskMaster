package com.aiagent.repository;

import com.aiagent.entity.LlmServer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmServerRepository extends JpaRepository<LlmServer, Long> {
    List<LlmServer> findByUserId(Long userId);
    List<LlmServer> findByUserIdAndIsActiveTrue(Long userId);
}
