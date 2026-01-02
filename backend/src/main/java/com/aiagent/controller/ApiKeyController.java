package com.aiagent.controller;

import com.aiagent.dto.apikey.ApiKeyCreatedResponse;
import com.aiagent.dto.apikey.ApiKeyRequest;
import com.aiagent.dto.apikey.ApiKeyResponse;
import com.aiagent.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/api-keys")
@RequiredArgsConstructor
@Tag(name = "API Keys", description = "API key management for external access")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;

    /**
     * Get all API keys for the authenticated user.
     */
    @Operation(summary = "Get all API keys", description = "Retrieve all API keys for the authenticated user")
    @ApiResponse(responseCode = "200", description = "List of API keys")
    @GetMapping
    public ResponseEntity<List<ApiKeyResponse>> getApiKeys(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(apiKeyService.getApiKeys(userId));
    }

    /**
     * Get active API keys for the authenticated user.
     */
    @Operation(summary = "Get active API keys", description = "Retrieve only active (non-revoked) API keys")
    @ApiResponse(responseCode = "200", description = "List of active API keys")
    @GetMapping("/active")
    public ResponseEntity<List<ApiKeyResponse>> getActiveApiKeys(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(apiKeyService.getActiveApiKeys(userId));
    }

    /**
     * Create a new API key.
     */
    @Operation(
            summary = "Create a new API key",
            description = "Create a new API key. The full key is only shown once in the response."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "API key created successfully",
                    content = @Content(schema = @Schema(implementation = ApiKeyCreatedResponse.class))),
            @ApiResponse(responseCode = "400", description = "Maximum number of API keys reached")
    })
    @PostMapping
    public ResponseEntity<ApiKeyCreatedResponse> createApiKey(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApiKeyRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(apiKeyService.createApiKey(userId, request));
    }

    /**
     * Update an existing API key.
     */
    @Operation(summary = "Update an API key", description = "Update API key name, description, or rate limit")
    @ApiResponse(responseCode = "200", description = "API key updated")
    @PutMapping("/{keyId}")
    public ResponseEntity<ApiKeyResponse> updateApiKey(
            @PathVariable Long keyId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ApiKeyRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(apiKeyService.updateApiKey(keyId, userId, request));
    }

    /**
     * Revoke (deactivate) an API key.
     */
    @Operation(summary = "Revoke an API key", description = "Deactivate an API key without deleting it")
    @ApiResponse(responseCode = "200", description = "API key revoked")
    @PostMapping("/{keyId}/revoke")
    public ResponseEntity<Map<String, String>> revokeApiKey(
            @PathVariable Long keyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        apiKeyService.revokeApiKey(keyId, userId);
        return ResponseEntity.ok(Map.of("message", "API key revoked successfully"));
    }

    /**
     * Delete an API key permanently.
     */
    @Operation(summary = "Delete an API key", description = "Permanently delete an API key")
    @ApiResponse(responseCode = "204", description = "API key deleted")
    @DeleteMapping("/{keyId}")
    public ResponseEntity<Void> deleteApiKey(
            @PathVariable Long keyId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        apiKeyService.deleteApiKey(keyId, userId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Extract user ID from UserDetails.
     */
    private Long getUserId(UserDetails userDetails) {
        if (userDetails instanceof com.aiagent.security.UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        throw new RuntimeException("Unable to extract user ID from authentication");
    }
}
