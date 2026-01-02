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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final LlmServerService llmServerService;
    private final AgentChatService agentChatService;
    // Wrap executor with DelegatingSecurityContextExecutorService to propagate security context
    private final ExecutorService executor = new DelegatingSecurityContextExecutorService(
            Executors.newCachedThreadPool()
    );

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
    public SseEmitter chatStream(
            @Valid @RequestBody ChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(300000L); // 5 minute timeout

        executor.execute(() -> {
            try {
                llmServerService.chatStream(
                        request.getServerId(),
                        principal.getId(),
                        request.getModel(),
                        request.getSystemPrompt(),
                        request.getUserMessage(),
                        request.getTemperature(),
                        request.getMaxTokens()
                ).doOnNext(data -> {
                    try {
                        emitter.send(SseEmitter.event().data(data));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }).doOnComplete(emitter::complete)
                  .doOnError(emitter::completeWithError)
                  .blockLast();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
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
    public SseEmitter generateStream(
            @Valid @RequestBody GenerateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(300000L);

        executor.execute(() -> {
            try {
                llmServerService.generateStream(
                        request.getServerId(),
                        principal.getId(),
                        request.getModel(),
                        request.getPrompt(),
                        request.getTemperature(),
                        request.getMaxTokens()
                ).doOnNext(data -> {
                    try {
                        emitter.send(SseEmitter.event().data(data));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }).doOnComplete(emitter::complete)
                  .doOnError(emitter::completeWithError)
                  .blockLast();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
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
    public SseEmitter agentChatStream(
            @PathVariable Long agentId,
            @Valid @RequestBody AgentChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        SseEmitter emitter = new SseEmitter(300000L);

        executor.execute(() -> {
            try {
                agentChatService.chatStream(agentId, principal.getId(), request)
                    .doOnNext(data -> {
                        try {
                            emitter.send(SseEmitter.event().data(data));
                        } catch (IOException e) {
                            log.error("Error sending SSE data: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    })
                    .doOnComplete(() -> {
                        log.debug("SSE stream completed");
                        emitter.complete();
                    })
                    .doOnError(error -> {
                        log.error("SSE stream error: {}", error.getMessage());
                        emitter.completeWithError(error);
                    })
                    .blockLast();
            } catch (Exception e) {
                log.error("SSE executor error: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }
}
