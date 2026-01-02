package com.aiagent.controller;

import com.aiagent.dto.chat.AgentChatRequest;
import com.aiagent.dto.chat.AgentChatResponse;
import com.aiagent.dto.external.ExternalApiError;
import com.aiagent.dto.external.ExternalChatRequest;
import com.aiagent.dto.external.ExternalChatResponse;
import com.aiagent.entity.Agent;
import com.aiagent.entity.ApiKey;
import com.aiagent.repository.AgentRepository;
import com.aiagent.security.ApiKeyAuthenticationFilter;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentChatService;
import com.aiagent.service.ApiUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.stream.Collectors;

/**
 * External API controller for agent interactions.
 * Uses API key authentication via X-API-Key header.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/external")
@RequiredArgsConstructor
@Tag(name = "External API", description = "External API for programmatic agent access using API keys")
@SecurityRequirement(name = "apiKeyAuth")
public class ExternalAgentController {

    private final AgentChatService agentChatService;
    private final AgentRepository agentRepository;
    private final ApiUsageService apiUsageService;
    private final ObjectMapper objectMapper;

    /**
     * Chat with an agent using its slug.
     * Requires API key authentication.
     */
    @Operation(
            summary = "Chat with an agent",
            description = "Send a message to an agent and receive a response. Supports conversation context via conversation_id."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Chat response received",
                    content = @Content(schema = @Schema(implementation = ExternalChatResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(implementation = ExternalApiError.class))),
            @ApiResponse(responseCode = "403", description = "Access denied to agent",
                    content = @Content(schema = @Schema(implementation = ExternalApiError.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found",
                    content = @Content(schema = @Schema(implementation = ExternalApiError.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error",
                    content = @Content(schema = @Schema(implementation = ExternalApiError.class)))
    })
    @PostMapping("/agents/{slug}/chat")
    public ResponseEntity<?> chat(
            @Parameter(description = "Agent slug identifier") @PathVariable String slug,
            @Valid @RequestBody ExternalChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ExternalApiError.unauthorized("API key required", httpRequest.getRequestURI()));
        }

        // Find agent by slug
        Agent agent = agentRepository.findBySlugWithAllRelations(slug).orElse(null);

        if (agent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ExternalApiError.notFound("Agent not found: " + slug, httpRequest.getRequestURI()));
        }

        // Check if user has access to this agent
        if (!agent.getUser().getId().equals(userPrincipal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ExternalApiError.forbidden("Access denied to agent: " + slug, httpRequest.getRequestURI()));
        }

        try {
            long startTime = System.currentTimeMillis();

            // Convert to internal request format
            AgentChatRequest chatRequest = AgentChatRequest.builder()
                    .message(request.getMessage())
                    .conversationId(request.getConversationId())
                    .build();

            // Execute chat
            AgentChatResponse chatResponse = agentChatService.chat(
                    agent.getId(), userPrincipal.getId(), chatRequest);

            long executionTime = System.currentTimeMillis() - startTime;

            // Convert to external response format
            ExternalChatResponse response = ExternalChatResponse.builder()
                    .response(chatResponse.getResponse())
                    .conversationId(chatResponse.getConversationId())
                    .model(chatResponse.getModel())
                    .sources(mapSources(chatResponse))
                    .functionCalls(mapFunctionCalls(chatResponse))
                    .usage(ExternalChatResponse.Usage.builder()
                            .executionTimeMs(executionTime)
                            .build())
                    .build();

            log.info("External API chat for agent '{}' by user {} completed in {}ms",
                    slug, userPrincipal.getId(), executionTime);

            // Record usage
            recordUsage(httpRequest, agent, 200, executionTime, null);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("External API chat error for agent '{}': {}", slug, e.getMessage());

            // Record failed usage
            recordUsage(httpRequest, agent, 500, null, e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ExternalApiError.internalError(e.getMessage(), httpRequest.getRequestURI()));
        }
    }

    /**
     * Chat with an agent using streaming (SSE).
     */
    @Operation(
            summary = "Chat with an agent (streaming)",
            description = "Send a message to an agent and receive a streaming response via Server-Sent Events."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Streaming response started"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API key"),
            @ApiResponse(responseCode = "404", description = "Agent not found")
    })
    @PostMapping(value = "/agents/{slug}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @Parameter(description = "Agent slug identifier") @PathVariable String slug,
            @Valid @RequestBody ExternalChatRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {

        if (userPrincipal == null) {
            return Flux.error(new RuntimeException("API key required"));
        }

        // Find agent by slug
        Agent agent = agentRepository.findBySlugWithAllRelations(slug).orElse(null);

        if (agent == null) {
            return Flux.error(new RuntimeException("Agent not found: " + slug));
        }

        // Check if user has access to this agent
        if (!agent.getUser().getId().equals(userPrincipal.getId())) {
            return Flux.error(new RuntimeException("Access denied to agent: " + slug));
        }

        // Convert to internal request format
        AgentChatRequest chatRequest = AgentChatRequest.builder()
                .message(request.getMessage())
                .conversationId(request.getConversationId())
                .build();

        log.info("External API streaming chat started for agent '{}' by user {}",
                slug, userPrincipal.getId());

        return agentChatService.chatStream(agent.getId(), userPrincipal.getId(), chatRequest);
    }

    /**
     * Get agent info by slug.
     */
    @Operation(
            summary = "Get agent information",
            description = "Retrieve basic information about an agent by its slug."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Agent information retrieved",
                    content = @Content(schema = @Schema(implementation = AgentInfo.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing API key",
                    content = @Content(schema = @Schema(implementation = ExternalApiError.class))),
            @ApiResponse(responseCode = "404", description = "Agent not found",
                    content = @Content(schema = @Schema(implementation = ExternalApiError.class)))
    })
    @GetMapping("/agents/{slug}")
    public ResponseEntity<?> getAgentInfo(
            @Parameter(description = "Agent slug identifier") @PathVariable String slug,
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            HttpServletRequest httpRequest) {

        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ExternalApiError.unauthorized("API key required", httpRequest.getRequestURI()));
        }

        Agent agent = agentRepository.findBySlug(slug).orElse(null);

        if (agent == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ExternalApiError.notFound("Agent not found: " + slug, httpRequest.getRequestURI()));
        }

        if (!agent.getUser().getId().equals(userPrincipal.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ExternalApiError.forbidden("Access denied to agent: " + slug, httpRequest.getRequestURI()));
        }

        return ResponseEntity.ok(AgentInfo.builder()
                .slug(agent.getSlug())
                .name(agent.getName())
                .description(agent.getDescription())
                .model(agent.getModelName())
                .isActive(agent.getIsActive())
                .build());
    }

    private java.util.List<ExternalChatResponse.Source> mapSources(AgentChatResponse response) {
        if (response.getSources() == null) {
            return null;
        }
        return response.getSources().stream()
                .map(s -> ExternalChatResponse.Source.builder()
                        .documentId(s.getDocumentId())
                        .documentName(s.getDocumentName())
                        .score(s.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    private java.util.List<ExternalChatResponse.FunctionCall> mapFunctionCalls(AgentChatResponse response) {
        if (response.getFunctionExecutions() == null) {
            return null;
        }
        return response.getFunctionExecutions().stream()
                .map(f -> {
                    String argsJson = null;
                    if (f.getArguments() != null) {
                        try {
                            argsJson = objectMapper.writeValueAsString(f.getArguments());
                        } catch (JsonProcessingException e) {
                            argsJson = "{}";
                        }
                    }
                    return ExternalChatResponse.FunctionCall.builder()
                            .name(f.getFunctionName())
                            .arguments(argsJson)
                            .result(f.getResult())
                            .executionTimeMs(f.getExecutionTimeMs())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void recordUsage(HttpServletRequest request, Agent agent, int status,
                              Long executionTimeMs, String errorMessage) {
        ApiKey apiKey = (ApiKey) request.getAttribute(ApiKeyAuthenticationFilter.API_KEY_ATTRIBUTE);
        if (apiKey != null) {
            apiUsageService.recordUsage(
                    apiKey,
                    agent,
                    request.getRequestURI(),
                    request.getMethod(),
                    status,
                    executionTimeMs,
                    null, // Request size - can be calculated from request if needed
                    null, // Response size
                    getClientIpAddress(request),
                    request.getHeader("User-Agent"),
                    errorMessage
            );
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AgentInfo {
        private String slug;
        private String name;
        private String description;
        private String model;
        private Boolean isActive;
    }
}
