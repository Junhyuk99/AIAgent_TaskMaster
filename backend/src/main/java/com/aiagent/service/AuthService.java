package com.aiagent.service;

import com.aiagent.dto.auth.AuthResponse;
import com.aiagent.dto.auth.LoginRequest;
import com.aiagent.dto.auth.RefreshTokenRequest;
import com.aiagent.dto.auth.RegisterRequest;
import com.aiagent.entity.User;
import com.aiagent.repository.UserRepository;
import com.aiagent.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        user = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());
            String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

            return buildAuthResponse(user, accessToken, refreshToken);
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new BadCredentialsException("Invalid token type");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getId());

        return buildAuthResponse(user, newAccessToken, refreshToken);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .name(user.getName())
                        .role(user.getRole().name())
                        .build())
                .build();
    }
}
