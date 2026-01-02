package com.aiagent.util;

import com.aiagent.entity.*;

import java.util.UUID;

/**
 * Factory class for creating test data entities.
 */
public class TestDataFactory {

    private TestDataFactory() {
    }

    public static User createUser() {
        return createUser("test@example.com");
    }

    public static User createUser(String email) {
        return User.builder()
                .email(email)
                .password("$2a$10$encoded_password_hash")
                .name("Test User")
                .isActive(true)
                .role(User.Role.USER)
                .build();
    }

    public static User createAdmin() {
        return User.builder()
                .email("admin@example.com")
                .password("$2a$10$encoded_password_hash")
                .name("Admin User")
                .isActive(true)
                .role(User.Role.ADMIN)
                .build();
    }

    public static Agent createAgent(User user) {
        return createAgent(user, "Test Agent");
    }

    public static Agent createAgent(User user, String name) {
        return Agent.builder()
                .name(name)
                .slug("test-agent-" + UUID.randomUUID().toString().substring(0, 8))
                .description("Test agent description")
                .systemPrompt("You are a helpful assistant.")
                .modelName("gpt-4")
                .temperature(0.7)
                .maxTokens(2048)
                .isActive(true)
                .user(user)
                .build();
    }

    public static LlmServer createLlmServer() {
        return createLlmServer("Test LLM Server");
    }

    public static LlmServer createLlmServer(String name) {
        return LlmServer.builder()
                .name(name)
                .type(LlmServer.LlmType.OLLAMA)
                .baseUrl("http://localhost:11434")
                .isActive(true)
                .build();
    }

    public static Function createFunction(User user) {
        return createFunction(user, "testFunction");
    }

    public static Function createFunction(User user, String name) {
        return Function.builder()
                .name(name)
                .description("A test function")
                .parametersSchema("{\"type\":\"object\",\"properties\":{}}")
                .returnType("string")
                .implementationType(Function.ImplementationType.HTTP_API)
                .implementationConfig("{\"method\":\"GET\",\"url\":\"https://api.example.com/test\"}")
                .isActive(true)
                .user(user)
                .build();
    }

    public static KnowledgeBase createKnowledgeBase(User user) {
        return createKnowledgeBase(user, "Test Knowledge Base");
    }

    public static KnowledgeBase createKnowledgeBase(User user, String name) {
        return KnowledgeBase.builder()
                .name(name)
                .description("Test knowledge base description")
                .chunkSize(1000)
                .chunkOverlap(200)
                .chunkingStrategy(KnowledgeBase.ChunkingStrategy.FIXED_SIZE)
                .isActive(true)
                .user(user)
                .build();
    }

    public static Conversation createConversation(Agent agent, User user) {
        return Conversation.builder()
                .title("Test Conversation")
                .agent(agent)
                .user(user)
                .build();
    }

    public static Message createMessage(Conversation conversation, String content, Message.Role role) {
        return Message.builder()
                .role(role)
                .content(content)
                .conversation(conversation)
                .build();
    }

    public static ApiKey createApiKey(User user) {
        return ApiKey.builder()
                .name("Test API Key")
                .keyHash("test_key_hash_" + UUID.randomUUID())
                .keyPrefix("test_")
                .isActive(true)
                .user(user)
                .build();
    }
}
