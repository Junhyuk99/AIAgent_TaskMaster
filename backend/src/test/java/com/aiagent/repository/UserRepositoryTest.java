package com.aiagent.repository;

import com.aiagent.base.BaseRepositoryTest;
import com.aiagent.entity.User;
import com.aiagent.util.TestDataFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("UserRepository Tests")
class UserRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should save and find user by email")
    void shouldFindUserByEmail() {
        // Given
        User user = TestDataFactory.createUser("test@example.com");
        userRepository.save(user);

        // When
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getName()).isEqualTo("Test User");
    }

    @Test
    @DisplayName("Should return empty when user not found")
    void shouldReturnEmptyWhenUserNotFound() {
        // When
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should check if email exists")
    void shouldCheckEmailExists() {
        // Given
        User user = TestDataFactory.createUser("existing@example.com");
        userRepository.save(user);

        // Then
        assertThat(userRepository.existsByEmail("existing@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("notexisting@example.com")).isFalse();
    }
}
