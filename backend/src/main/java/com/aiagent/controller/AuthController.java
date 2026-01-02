package com.aiagent.controller;

import com.aiagent.dto.auth.AuthResponse;
import com.aiagent.dto.auth.LoginRequest;
import com.aiagent.dto.auth.RefreshTokenRequest;
import com.aiagent.dto.auth.RegisterRequest;
import com.aiagent.security.UserPrincipal;
import com.aiagent.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public ResponseEntity<AuthResponse.UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        AuthResponse.UserResponse user = AuthResponse.UserResponse.builder()
                .id(userPrincipal.getId())
                .email(userPrincipal.getEmail())
                .name(userPrincipal.getName())
                .role(userPrincipal.getAuthorities().iterator().next().getAuthority().replace("ROLE_", ""))
                .build();
        return ResponseEntity.ok(user);
    }
}
