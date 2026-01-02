package com.aiagent.controller;

import com.aiagent.dto.agent.AgentRequest;
import com.aiagent.dto.agent.AgentResponse;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public ResponseEntity<List<AgentResponse>> getAllAgents(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.getAllAgents(principal.getId()));
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<AgentResponse>> getAgentsPaged(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(agentService.getAgentsPaged(principal.getId(), pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AgentResponse>> getActiveAgents(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.getActiveAgents(principal.getId()));
    }

    @GetMapping("/search")
    public ResponseEntity<List<AgentResponse>> searchAgents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String query) {
        return ResponseEntity.ok(agentService.searchAgents(principal.getId(), query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AgentResponse> getAgent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.getAgent(id, principal.getId()));
    }

    @PostMapping
    public ResponseEntity<AgentResponse> createAgent(
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentResponse response = agentService.createAgent(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AgentResponse> updateAgent(
            @PathVariable Long id,
            @Valid @RequestBody AgentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.updateAgent(id, request, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        agentService.deleteAgent(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-active")
    public ResponseEntity<AgentResponse> toggleActive(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.toggleActive(id, principal.getId()));
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<AgentResponse> duplicateAgent(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentResponse response = agentService.duplicateAgent(id, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/generate-slug")
    public ResponseEntity<AgentResponse> generateSlug(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.generateSlug(id, principal.getId()));
    }

    @PutMapping("/{id}/functions")
    public ResponseEntity<AgentResponse> updateFunctions(
            @PathVariable Long id,
            @RequestBody List<Long> functionIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.updateFunctions(id, functionIds, principal.getId()));
    }

    @PutMapping("/{id}/knowledge-bases")
    public ResponseEntity<AgentResponse> updateKnowledgeBases(
            @PathVariable Long id,
            @RequestBody List<Long> knowledgeBaseIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(agentService.updateKnowledgeBases(id, knowledgeBaseIds, principal.getId()));
    }
}
