package com.aiagent.config;

import com.aiagent.security.ApiKeyAuthenticationFilter;
import com.aiagent.security.JwtAuthenticationFilter;
import com.aiagent.security.ratelimit.RateLimitFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("docker")
public class SecurityConfigNoOAuth {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow ASYNC dispatch without re-authentication (for SSE streaming)
                .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                .requestMatchers(
                    "/api/auth/**",
                    "/api/health",
                    "/actuator/health",
                    "/actuator/info",
                    "/swagger-ui/**",
                    "/api-docs/**",
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, ApiKeyAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
