package com.aiagent.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Content Security Policy
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self' data:; " +
                "connect-src 'self' http://localhost:* https://localhost:*; " +
                "frame-ancestors 'none'");

        // X-Content-Type-Options
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // X-XSS-Protection (legacy but still useful)
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer-Policy
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions-Policy
        httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // Cache-Control for API responses
        if (request instanceof jakarta.servlet.http.HttpServletRequest httpRequest) {
            String path = httpRequest.getRequestURI();
            if (path.startsWith("/api/")) {
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
                httpResponse.setHeader("Pragma", "no-cache");
            }
        }

        chain.doFilter(request, response);
    }
}
