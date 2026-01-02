package com.aiagent.controller;

import com.aiagent.dto.llm.ConnectionTestResponse;
import com.aiagent.dto.llm.LlmServerRequest;
import com.aiagent.dto.llm.LlmServerResponse;
import com.aiagent.dto.llm.ModelInfo;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.LlmServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm-servers")
@RequiredArgsConstructor
public class LlmServerController {

    private final LlmServerService llmServerService;

    @GetMapping
    public ResponseEntity<List<LlmServerResponse>> getAllServers(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.getAllServers(principal.getId()));
    }

    @GetMapping("/active")
    public ResponseEntity<List<LlmServerResponse>> getActiveServers(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.getActiveServers(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LlmServerResponse> getServer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.getServer(id, principal.getId()));
    }

    @PostMapping
    public ResponseEntity<LlmServerResponse> createServer(
            @Valid @RequestBody LlmServerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        LlmServerResponse response = llmServerService.createServer(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LlmServerResponse> updateServer(
            @PathVariable Long id,
            @Valid @RequestBody LlmServerRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.updateServer(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        llmServerService.deleteServer(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<LlmServerResponse> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.toggleActive(id, principal.getId()));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<ConnectionTestResponse> testConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.testConnection(id, principal.getId()));
    }

    @GetMapping("/{id}/models")
    public ResponseEntity<List<ModelInfo>> getModels(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(llmServerService.getModels(id, principal.getId()));
    }
}
