package com.aiagent.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Filter that applies IP-based rate limiting for unauthenticated requests.
 * This filter runs before authentication to protect public endpoints.
 */
@Slf4j
@Component
@Order(1) // Run early in the filter chain
@RequiredArgsConstructor
public class IpRateLimitFilter extends OncePerRequestFilter {

    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RATE_LIMIT_RESET_HEADER = "X-RateLimit-Reset";

    private static final Set<String> EXEMPT_PATHS = Set.of(
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui",
            "/api-docs",
            "/v3/api-docs"
    );

    private final IpRateLimiter ipRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip if rate limiting is disabled
        if (!ipRateLimiter.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip exempt paths
        String path = request.getRequestURI();
        if (isExemptPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip if already authenticated (API key rate limiting will handle it)
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIpAddress(request);

        // Add rate limit headers
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf(ipRateLimiter.getLimit()));

        if (!ipRateLimiter.isAllowed(clientIp)) {
            // Rate limited
            int remaining = ipRateLimiter.getRemainingRequests(clientIp);
            long resetTime = ipRateLimiter.getResetTimeSeconds(clientIp);

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
            log.warn("IP rate limit exceeded for {}", clientIp);
            return;
        }

        int remaining = ipRateLimiter.getRemainingRequests(clientIp);
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    private boolean isExemptPath(String path) {
        return EXEMPT_PATHS.stream().anyMatch(path::startsWith);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
