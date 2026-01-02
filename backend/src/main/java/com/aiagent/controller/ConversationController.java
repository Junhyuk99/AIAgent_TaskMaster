package com.aiagent.controller;

import com.aiagent.dto.conversation.ConversationListResponse;
import com.aiagent.dto.conversation.ConversationResponse;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /**
     * Get all conversations for the current user.
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationResponse>> getAllConversations(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(conversationService.getConversationsByUser(user.getId()));
    }

    /**
     * Get conversations for a specific agent.
     */
    @GetMapping("/agents/{agentId}/conversations")
    public ResponseEntity<List<ConversationResponse>> getAgentConversations(
            @PathVariable Long agentId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(conversationService.getConversationsByAgent(agentId, user.getId()));
    }

    /**
     * Get paginated conversations for an agent.
     */
    @GetMapping("/agents/{agentId}/conversations/paged")
    public ResponseEntity<ConversationListResponse> getAgentConversationsPaged(
            @PathVariable Long agentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(conversationService.getConversationsByAgentPaged(agentId, user.getId(), page, size));
    }

    /**
     * Get a specific conversation with messages.
     */
    @GetMapping("/conversations/{externalId}")
    public ResponseEntity<ConversationResponse> getConversation(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(conversationService.getConversation(externalId, user.getId()));
    }

    /**
     * Update conversation title.
     */
    @PatchMapping("/conversations/{externalId}")
    public ResponseEntity<ConversationResponse> updateConversation(
            @PathVariable String externalId,
            @RequestBody Map<String, String> updates,
            @AuthenticationPrincipal UserPrincipal user) {
        String title = updates.get("title");
        if (title == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(conversationService.updateTitle(externalId, user.getId(), title));
    }

    /**
     * Archive a conversation.
     */
    @PostMapping("/conversations/{externalId}/archive")
    public ResponseEntity<Void> archiveConversation(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserPrincipal user) {
        conversationService.archiveConversation(externalId, user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a conversation.
     */
    @DeleteMapping("/conversations/{externalId}")
    public ResponseEntity<Void> deleteConversation(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserPrincipal user) {
        conversationService.deleteConversation(externalId, user.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Export conversation as JSON.
     */
    @GetMapping("/conversations/{externalId}/export")
    public ResponseEntity<String> exportConversation(
            @PathVariable String externalId,
            @AuthenticationPrincipal UserPrincipal user) {
        String json = conversationService.exportConversation(externalId, user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"conversation-" + externalId + ".json\"")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
    }
}
