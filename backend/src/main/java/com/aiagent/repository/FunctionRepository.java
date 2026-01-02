package com.aiagent.repository;

import com.aiagent.entity.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FunctionRepository extends JpaRepository<Function, Long> {
    List<Function> findByUserId(Long userId);
    Page<Function> findByUserId(Long userId, Pageable pageable);
    List<Function> findByUserIdAndIsActiveTrue(Long userId);
}
