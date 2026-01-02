package com.aiagent.security.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * In-memory rate limiter for IP addresses using sliding window algorithm.
 * Used for unauthenticated requests.
 */
@Slf4j
@Component
public class IpRateLimiter {

    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute sliding window

    @Value("${rate-limit.ip.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.ip.enabled:true}")
    private boolean enabled;

    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<Long>> requestTimestamps = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed for the given IP address.
     *
     * @param ipAddress The client IP address
     * @return true if request is allowed, false if rate limited
     */
    public boolean isAllowed(String ipAddress) {
        if (!enabled) {
            return true;
        }

        long now = Instant.now().toEpochMilli();
        long windowStart = now - WINDOW_SIZE_MS;

        ConcurrentLinkedDeque<Long> timestamps = requestTimestamps.computeIfAbsent(
                ipAddress, k -> new ConcurrentLinkedDeque<>());

        // Clean old timestamps outside the window
        while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
            timestamps.pollFirst();
        }

        // Check if under limit
        if (timestamps.size() < requestsPerMinute) {
            timestamps.addLast(now);
            return true;
        }

        log.warn("IP rate limit exceeded for {}: {} requests in window", ipAddress, timestamps.size());
        return false;
    }

    /**
     * Get remaining requests for the current window.
     */
    public int getRemainingRequests(String ipAddress) {
        if (!enabled) {
            return requestsPerMinute;
        }

        long windowStart = Instant.now().toEpochMilli() - WINDOW_SIZE_MS;

        ConcurrentLinkedDeque<Long> timestamps = requestTimestamps.get(ipAddress);
        if (timestamps == null) {
            return requestsPerMinute;
        }

        long count = timestamps.stream().filter(t -> t >= windowStart).count();
        return Math.max(0, requestsPerMinute - (int) count);
    }

    /**
     * Get seconds until rate limit resets.
     */
    public long getResetTimeSeconds(String ipAddress) {
        ConcurrentLinkedDeque<Long> timestamps = requestTimestamps.get(ipAddress);
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
     * Get the configured requests per minute limit.
     */
    public int getLimit() {
        return requestsPerMinute;
    }

    /**
     * Check if IP rate limiting is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Periodically clean up old entries to prevent memory leak.
     * Should be called by a scheduled task.
     */
    public void cleanup() {
        long windowStart = Instant.now().toEpochMilli() - WINDOW_SIZE_MS;

        requestTimestamps.entrySet().removeIf(entry -> {
            ConcurrentLinkedDeque<Long> timestamps = entry.getValue();
            timestamps.removeIf(t -> t < windowStart);
            return timestamps.isEmpty();
        });
    }
}
