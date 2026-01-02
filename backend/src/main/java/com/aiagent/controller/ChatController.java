package com.aiagent.controller;

import com.aiagent.dto.chat.AgentChatRequest;
import com.aiagent.dto.chat.AgentChatResponse;
import com.aiagent.dto.llm.ChatRequest;
import com.aiagent.dto.llm.GenerateRequest;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentChatService;
import com.aiagent.service.LlmServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LlmServerService llmServerService;
    private final AgentChatService agentChatService;

    @PostMapping
    public ResponseEntity<Map<String, String>> chat(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String response = llmServerService.chat(
                request.getServerId(),
                principal.getId(),
                request.getModel(),
                request.getSystemPrompt(),
                request.getUserMessage(),
                request.getTemperature(),
                request.getMaxTokens()
        );
        return ResponseEntity.ok(Map.of("response", response));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return llmServerService.chatStream(
                request.getServerId(),
                principal.getId(),
                request.getModel(),
                request.getSystemPrompt(),
                request.getUserMessage(),
                request.getTemperature(),
                request.getMaxTokens()
        );
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generate(
            @Valid @RequestBody GenerateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String response = llmServerService.generate(
                request.getServerId(),
                principal.getId(),
                request.getModel(),
                request.getPrompt(),
                request.getTemperature(),
                request.getMaxTokens()
        );
        return ResponseEntity.ok(Map.of("response", response));
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateStream(
            @Valid @RequestBody GenerateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return llmServerService.generateStream(
                request.getServerId(),
                principal.getId(),
                request.getModel(),
                request.getPrompt(),
                request.getTemperature(),
                request.getMaxTokens()
        );
    }

    // Agent-based chat endpoints

    @PostMapping("/agent/{agentId}")
    public ResponseEntity<AgentChatResponse> agentChat(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentChatResponse response = agentChatService.chat(agentId, principal.getId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/agent/{agentId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentChatStream(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return agentChatService.chatStream(agentId, principal.getId(), request);
    }
}
