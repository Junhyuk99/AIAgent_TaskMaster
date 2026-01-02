package com.aiagent.security;

import com.aiagent.entity.ApiKey;
import com.aiagent.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter for API key authentication.
 * Checks for X-API-Key header and authenticates the user if valid.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-Key";
    public static final String API_KEY_ATTRIBUTE = "API_KEY";

    private final ApiKeyService apiKeyService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Skip if already authenticated
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String apiKeyValue = getApiKeyFromRequest(request);

            if (StringUtils.hasText(apiKeyValue)) {
                Optional<ApiKey> apiKeyOpt = apiKeyService.getApiKeyForValidation(apiKeyValue);

                if (apiKeyOpt.isPresent()) {
                    ApiKey apiKey = apiKeyOpt.get();

                    if (apiKey.isValid()) {
                        UserDetails userDetails = userDetailsService.loadUserById(apiKey.getUser().getId());

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authentication);

                        // Store API key for usage tracking
                        request.setAttribute(API_KEY_ATTRIBUTE, apiKey);

                        // Update usage statistics
                        apiKeyService.incrementUsageCount(apiKey.getId());

                        log.debug("Authenticated user {} via API key '{}'",
                                apiKey.getUser().getId(), apiKey.getName());
                    } else {
                        log.warn("API key '{}' is expired or inactive", apiKey.getName());
                    }
                } else {
                    log.warn("Invalid API key provided");
                }
            }
        } catch (Exception ex) {
            log.error("Could not authenticate via API key", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getApiKeyFromRequest(HttpServletRequest request) {
        return request.getHeader(API_KEY_HEADER);
    }
}
