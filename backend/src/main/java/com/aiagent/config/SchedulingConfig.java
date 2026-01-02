package com.aiagent.config;

import com.aiagent.security.ratelimit.IpRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final IpRateLimiter ipRateLimiter;

    /**
     * Clean up expired rate limit entries every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void cleanupRateLimitEntries() {
        log.debug("Cleaning up expired rate limit entries");
        ipRateLimiter.cleanup();
    }
}
