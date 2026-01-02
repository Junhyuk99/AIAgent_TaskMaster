package com.aiagent.service;

import com.aiagent.dto.apikey.ApiKeyCreatedResponse;
import com.aiagent.dto.apikey.ApiKeyRequest;
import com.aiagent.dto.apikey.ApiKeyResponse;
import com.aiagent.entity.ApiKey;
import com.aiagent.entity.User;
import com.aiagent.repository.ApiKeyRepository;
import com.aiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String KEY_PREFIX = "aak_"; // AI Agent Key prefix
    private static final int KEY_LENGTH = 32; // bytes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${api-key.max-per-user:10}")
    private int maxKeysPerUser;

    @Value("${api-key.default-rate-limit:100}")
    private int defaultRateLimit;

    /**
     * Get all API keys for a user.
     */
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getApiKeys(Long userId) {
        return apiKeyRepository.findByUserId(userId).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    /**
     * Get active API keys for a user.
     */
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> getActiveApiKeys(Long userId) {
        return apiKeyRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(ApiKeyResponse::from)
                .toList();
    }

    /**
     * Create a new API key.
     */
    @Transactional
    public ApiKeyCreatedResponse createApiKey(Long userId, ApiKeyRequest request) {
        // Check max keys limit
        long activeKeyCount = apiKeyRepository.countActiveByUserId(userId);
        if (activeKeyCount >= maxKeysPerUser) {
            throw new RuntimeException("Maximum number of API keys (" + maxKeysPerUser + ") reached");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate unique key
        String fullKey = generateApiKey();
        String keyPrefix = fullKey.substring(0, KEY_PREFIX.length() + 8); // prefix + first 8 chars
        String keyHash = passwordEncoder.encode(fullKey);

        // Calculate expiration
        LocalDateTime expiresAt = null;
        if (request.getExpiresInDays() != null && request.getExpiresInDays() > 0) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpiresInDays());
        }

        ApiKey apiKey = ApiKey.builder()
                .name(request.getName())
                .description(request.getDescription())
                .keyPrefix(keyPrefix)
                .keyHash(keyHash)
                .user(user)
                .expiresAt(expiresAt)
                .rateLimit(request.getRateLimit() != null ? request.getRateLimit() : defaultRateLimit)
                .build();

        apiKey = apiKeyRepository.save(apiKey);
        log.info("Created API key '{}' for user {}", request.getName(), userId);

        return ApiKeyCreatedResponse.from(apiKey, fullKey);
    }

    /**
     * Revoke (deactivate) an API key.
     */
    @Transactional
    public void revokeApiKey(Long keyId, Long userId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(k -> k.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("API key not found"));

        apiKey.setIsActive(false);
        apiKeyRepository.save(apiKey);
        log.info("Revoked API key '{}' for user {}", apiKey.getName(), userId);
    }

    /**
     * Delete an API key permanently.
     */
    @Transactional
    public void deleteApiKey(Long keyId, Long userId) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(k -> k.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("API key not found"));

        apiKeyRepository.delete(apiKey);
        log.info("Deleted API key '{}' for user {}", apiKey.getName(), userId);
    }

    /**
     * Update API key details.
     */
    @Transactional
    public ApiKeyResponse updateApiKey(Long keyId, Long userId, ApiKeyRequest request) {
        ApiKey apiKey = apiKeyRepository.findById(keyId)
                .filter(k -> k.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("API key not found"));

        if (request.getName() != null) {
            apiKey.setName(request.getName());
        }
        if (request.getDescription() != null) {
            apiKey.setDescription(request.getDescription());
        }
        if (request.getRateLimit() != null) {
            apiKey.setRateLimit(request.getRateLimit());
        }

        apiKey = apiKeyRepository.save(apiKey);
        return ApiKeyResponse.from(apiKey);
    }

    /**
     * Validate an API key and return the associated user ID.
     */
    @Transactional
    public Optional<Long> validateApiKey(String fullKey) {
        if (fullKey == null || !fullKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        String keyPrefix = fullKey.substring(0, KEY_PREFIX.length() + 8);
        List<ApiKey> candidates = apiKeyRepository.findActiveByKeyPrefixWithUser(keyPrefix);

        for (ApiKey apiKey : candidates) {
            if (passwordEncoder.matches(fullKey, apiKey.getKeyHash())) {
                if (!apiKey.isValid()) {
                    log.warn("API key '{}' is expired or inactive", apiKey.getName());
                    return Optional.empty();
                }

                // Update usage statistics
                apiKey.incrementUsageCount();
                apiKeyRepository.save(apiKey);

                return Optional.of(apiKey.getUser().getId());
            }
        }

        return Optional.empty();
    }

    /**
     * Get API key details by ID for validation/rate limiting.
     */
    @Transactional(readOnly = true)
    public Optional<ApiKey> getApiKeyForValidation(String fullKey) {
        if (fullKey == null || !fullKey.startsWith(KEY_PREFIX)) {
            return Optional.empty();
        }

        String keyPrefix = fullKey.substring(0, KEY_PREFIX.length() + 8);
        List<ApiKey> candidates = apiKeyRepository.findActiveByKeyPrefixWithUser(keyPrefix);

        for (ApiKey apiKey : candidates) {
            if (passwordEncoder.matches(fullKey, apiKey.getKeyHash())) {
                return Optional.of(apiKey);
            }
        }

        return Optional.empty();
    }

    /**
     * Generate a secure random API key.
     */
    private String generateApiKey() {
        byte[] bytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return KEY_PREFIX + randomPart;
    }

    /**
     * Increment usage count for an API key.
     */
    @Transactional
    public void incrementUsageCount(Long apiKeyId) {
        apiKeyRepository.findById(apiKeyId).ifPresent(apiKey -> {
            apiKey.incrementUsageCount();
            apiKeyRepository.save(apiKey);
        });
    }
}
