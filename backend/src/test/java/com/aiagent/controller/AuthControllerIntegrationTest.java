package com.aiagent.controller;

import com.aiagent.base.BaseIntegrationTest;
import com.aiagent.dto.auth.RegisterRequest;
import com.aiagent.dto.auth.LoginRequest;
import com.aiagent.entity.User;
import com.aiagent.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("Should register new user successfully")
        void shouldRegisterNewUser() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("newuser@example.com");
            request.setPassword("password123");
            request.setName("New User");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.user.email").value("newuser@example.com"));
        }

        @Test
        @DisplayName("Should fail when email already exists")
        void shouldFailWhenEmailExists() throws Exception {
            // Create existing user
            User existingUser = User.builder()
                    .email("existing@example.com")
                    .password(passwordEncoder.encode("password"))
                    .name("Existing User")
                    .build();
            userRepository.save(existingUser);

            RegisterRequest request = new RegisterRequest();
            request.setEmail("existing@example.com");
            request.setPassword("password123");
            request.setName("New User");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("Should login with valid credentials")
        void shouldLoginWithValidCredentials() throws Exception {
            // Create user
            User user = User.builder()
                    .email("user@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .name("Test User")
                    .build();
            userRepository.save(user);

            LoginRequest request = new LoginRequest();
            request.setEmail("user@example.com");
            request.setPassword("password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists());
        }

        @Test
        @DisplayName("Should fail with invalid credentials")
        void shouldFailWithInvalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("nonexistent@example.com");
            request.setPassword("wrongpassword");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(asJsonString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
