package com.aiagent.controller;

import com.aiagent.dto.AgentVersionResponse;
import com.aiagent.dto.agent.AgentResponse;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AgentVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agents/{agentId}/versions")
@RequiredArgsConstructor
public class AgentVersionController {

    private final AgentVersionService agentVersionService;

    @GetMapping
    public ResponseEntity<List<AgentVersionResponse>> getVersionHistory(
            @PathVariable Long agentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AgentVersionResponse> versions = agentVersionService.getVersionHistory(agentId, principal.getId());
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/paged")
    public ResponseEntity<Page<AgentVersionResponse>> getVersionHistoryPaged(
            @PathVariable Long agentId,
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        Page<AgentVersionResponse> versions = agentVersionService.getVersionHistoryPaged(agentId, principal.getId(), pageable);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{versionNumber}")
    public ResponseEntity<AgentVersionResponse> getVersion(
            @PathVariable Long agentId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentVersionResponse version = agentVersionService.getVersion(agentId, versionNumber, principal.getId());
        return ResponseEntity.ok(version);
    }

    @PostMapping("/{versionNumber}/rollback")
    public ResponseEntity<AgentResponse> rollbackToVersion(
            @PathVariable Long agentId,
            @PathVariable Integer versionNumber,
            @AuthenticationPrincipal UserPrincipal principal) {
        var agent = agentVersionService.rollbackToVersion(agentId, versionNumber, principal.getId());
        return ResponseEntity.ok(AgentResponse.from(agent));
    }

    @GetMapping("/compare")
    public ResponseEntity<Map<String, AgentVersionResponse>> compareVersions(
            @PathVariable Long agentId,
            @RequestParam Integer v1,
            @RequestParam Integer v2,
            @AuthenticationPrincipal UserPrincipal principal) {
        AgentVersionResponse version1 = agentVersionService.getVersion(agentId, v1, principal.getId());
        AgentVersionResponse version2 = agentVersionService.getVersion(agentId, v2, principal.getId());
        return ResponseEntity.ok(Map.of("version1", version1, "version2", version2));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getVersionCount(@PathVariable Long agentId) {
        long count = agentVersionService.getVersionCount(agentId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
