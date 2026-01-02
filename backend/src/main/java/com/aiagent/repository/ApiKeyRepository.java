package com.aiagent.repository;

import com.aiagent.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByUserId(Long userId);

    List<ApiKey> findByUserIdAndIsActiveTrue(Long userId);

    @Query("SELECT a FROM ApiKey a WHERE a.keyPrefix = :prefix AND a.isActive = true")
    List<ApiKey> findActiveByKeyPrefix(@Param("prefix") String prefix);

    @Query("SELECT a FROM ApiKey a WHERE a.keyHash = :hash AND a.isActive = true")
    Optional<ApiKey> findByKeyHash(@Param("hash") String hash);

    @Query("SELECT a FROM ApiKey a JOIN FETCH a.user WHERE a.keyPrefix = :prefix AND a.isActive = true")
    List<ApiKey> findActiveByKeyPrefixWithUser(@Param("prefix") String prefix);

    @Query("SELECT COUNT(a) FROM ApiKey a WHERE a.user.id = :userId AND a.isActive = true")
    long countActiveByUserId(@Param("userId") Long userId);

    boolean existsByKeyHash(String keyHash);
}
