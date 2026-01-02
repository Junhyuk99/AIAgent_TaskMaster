package com.aiagent.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory rate limiter for API keys using sliding window algorithm.
 * Each API key has a configured rate limit (requests per minute).
 */
@Slf4j
@Component
public class ApiKeyRateLimiter {

    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute sliding window

    private final ConcurrentHashMap<Long, ConcurrentLinkedDeque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed for the given API key.
     *
     * @param apiKeyId  The API key ID
     * @param rateLimit Maximum requests per minute
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(Long apiKeyId, int rateLimit) {
        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_SIZE_MS;

        ConcurrentLinkedDeque<Long> timestamps = requestTimestamps.computeIfAbsent(
                apiKeyId, k -> new ConcurrentLinkedDeque<>());

        // Clean old timestamps outside the window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        // Check if under limit
        if (timestamps.size() < rateLimit) {
            timestamps.addLast(now);
            return true;
        }

        log.warn("Rate limit exceeded for API key {}: {} requests in window", apiKeyId, timestamps.size());
        return false;
    }

    /**
     * Get remaining requests for the current window.
     *
     * @param apiKeyId  The API key ID
     * @param rateLimit Maximum requests per minute
     * @return Number of remaining requests
     */
    public int getRemainingRequests(Long apiKeyId, int rateLimit) {
        long windowStart = Instant.now().toEpochMilli() - WINDOW_SIZE_MS;

        ConcurrentLinkedDeque<Long> timestamps = requestTimestamps.get(apiKeyId);
        if (timestamps == null) {
            return rateLimit;
        }

        // Count valid timestamps within the window
        long count = timestamps.stream().filter(t -> t >= windowStart).count();
        return Math.max(0, rateLimit - (int) count);
    }

    /**
     * Get seconds until rate limit resets.
     *
     * @param apiKeyId The API key ID
     * @return Seconds until oldest request expires from window
     */
    public long getResetTimeSeconds(Long apiKeyId) {
        ConcurrentLinkedDeque<Long> timestamps = requestTimestamps.get(apiKeyId);
        if (timestamps == null || timestamps.isEmpty()) {
            return 0;
        }

        Long oldest = timestamps.peekFirst();
        if (oldest == null) {
            return 0;
        }

        long expiresAt = oldest + WINDOW_SIZE_MS;
        long now = Instant.now().toEpochMilli();
        return Math.max(0, (expiresAt - now) / 1000);
    }

    /**
     * Clear rate limit data for an API key.
     */
    public void clearRateLimit(Long apiKeyId) {
        requestTimestamps.remove(apiKeyId);
    }
}
