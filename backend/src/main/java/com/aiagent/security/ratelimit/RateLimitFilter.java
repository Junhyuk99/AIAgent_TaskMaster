package com.aiagent.security.ratelimit;

import com.aiagent.entity.ApiKey;
import com.aiagent.service.ApiKeyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Filter that applies rate limiting for API key authenticated requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private final ApiKeyService apiKeyService;
    private final ApiKeyRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);

        // Only apply rate limiting for API key authenticated requests
        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiKey> apiKeyEntity = apiKeyService.getApiKeyForValidation(apiKey);

        if (apiKeyEntity.isEmpty()) {
            // Invalid API key - let the auth filter handle it
            filterChain.doFilter(request, response);
            return;
        }

        ApiKey key = apiKeyEntity.get();
        int rateLimit = key.getRateLimit();

        // Add rate limit headers
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(rateLimit));

        if (!rateLimiter.isAllowed(key.getId(), rateLimit)) {
            // Rate limited
            int remaining = rateLimiter.getRemainingRequests(key.getId(), rateLimit);
            long resetTime = rateLimiter.getResetTimeSeconds(key.getId());

            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));
            response.setHeader(RATE_LIMIT_RESET_HEADER, String.valueOf(resetTime));
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorResponse = Map.of(
                    "error", "Too Many Requests",
                    "message", "Rate limit exceeded. Please try again in " + resetTime + " seconds.",
                    "retryAfter", resetTime
            );

            objectMapper.writeValue(response.getOutputStream(), errorResponse);
            log.warn("Rate limit exceeded for API key '{}' (id: {})", key.getName(), key.getId());
            return;
        }

        int remaining = rateLimiter.getRemainingRequests(key.getId(), rateLimit);
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }
}
