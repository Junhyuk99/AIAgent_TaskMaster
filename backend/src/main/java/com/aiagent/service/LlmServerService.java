package com.aiagent.service;

import com.aiagent.connector.LlmConnector;
import com.aiagent.connector.OllamaConnector;
import com.aiagent.connector.OpenAICompatibleConnector;
import com.aiagent.dto.llm.ConnectionTestResponse;
import com.aiagent.dto.llm.LlmServerRequest;
import com.aiagent.dto.llm.LlmServerResponse;
import com.aiagent.dto.llm.ModelInfo;
import com.aiagent.entity.LlmServer;
import com.aiagent.entity.User;
import com.aiagent.repository.LlmServerRepository;
import com.aiagent.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
public class LlmServerService {

    private final LlmServerRepository llmServerRepository;
    private final UserRepository userRepository;

    // Ollama configuration
    private final int ollamaNumCtx;
    private final int ollamaTimeout;
    private final int ollamaRetryCount;

    public LlmServerService(
            LlmServerRepository llmServerRepository,
            UserRepository userRepository,
            @Value("${ollama.num-ctx:8192}") int ollamaNumCtx,
            @Value("${ollama.timeout:180}") int ollamaTimeout,
            @Value("${ollama.retry-count:3}") int ollamaRetryCount) {
        this.llmServerRepository = llmServerRepository;
        this.userRepository = userRepository;
        this.ollamaNumCtx = ollamaNumCtx;
        this.ollamaTimeout = ollamaTimeout;
        this.ollamaRetryCount = ollamaRetryCount;
        log.info("LlmServerService initialized with Ollama settings: numCtx={}, timeout={}s, retryCount={}",
                ollamaNumCtx, ollamaTimeout, ollamaRetryCount);
    }

    @Transactional(readOnly = true)
    public List<LlmServerResponse> getAllServers(Long userId) {
        return llmServerRepository.findByUserId(userId).stream()
                .map(LlmServerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LlmServerResponse> getActiveServers(Long userId) {
        return llmServerRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(LlmServerResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LlmServerResponse getServer(Long id, Long userId) {
        LlmServer server = findServerByIdAndUser(id, userId);
        return LlmServerResponse.from(server);
    }

    @Transactional
    public LlmServerResponse createServer(LlmServerRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        LlmServer server = LlmServer.builder()
                .name(request.getName())
                .type(request.getType())
                .baseUrl(normalizeBaseUrl(request.getBaseUrl()))
                .apiKey(request.getApiKey())
                .isActive(true)
                .user(user)
                .build();

        LlmServer saved = llmServerRepository.save(server);
        log.info("Created LLM server: {} for user: {}", saved.getName(), userId);
        return LlmServerResponse.from(saved);
    }

    @Transactional
    public LlmServerResponse updateServer(Long id, LlmServerRequest request, Long userId) {
        LlmServer server = findServerByIdAndUser(id, userId);

        server.setName(request.getName());
        server.setType(request.getType());
        server.setBaseUrl(normalizeBaseUrl(request.getBaseUrl()));

        if (request.getApiKey() != null) {
            server.setApiKey(request.getApiKey());
        }

        LlmServer updated = llmServerRepository.save(server);
        log.info("Updated LLM server: {}", updated.getName());
        return LlmServerResponse.from(updated);
    }

    @Transactional
    public void deleteServer(Long id, Long userId) {
        LlmServer server = findServerByIdAndUser(id, userId);
        llmServerRepository.delete(server);
        log.info("Deleted LLM server: {}", server.getName());
    }

    @Transactional
    public LlmServerResponse toggleActive(Long id, Long userId) {
        LlmServer server = findServerByIdAndUser(id, userId);
        server.setIsActive(!server.getIsActive());
        LlmServer updated = llmServerRepository.save(server);
        log.info("Toggled LLM server active status: {} -> {}", server.getName(), updated.getIsActive());
        return LlmServerResponse.from(updated);
    }

    public ConnectionTestResponse testConnection(Long id, Long userId) {
        LlmServer server = findServerByIdAndUser(id, userId);
        LlmConnector connector = createConnector(server);
        return connector.testConnection();
    }

    public List<ModelInfo> getModels(Long id, Long userId) {
        LlmServer server = findServerByIdAndUser(id, userId);
        LlmConnector connector = createConnector(server);
        return connector.getModels();
    }

    public String chat(Long serverId, Long userId, String model, String systemPrompt,
                       String userMessage, Double temperature, Integer maxTokens) {
        LlmServer server = findServerByIdAndUser(serverId, userId);
        LlmConnector connector = createConnector(server);
        return connector.chat(model, systemPrompt, userMessage, temperature, maxTokens);
    }

    public Flux<String> chatStream(Long serverId, Long userId, String model, String systemPrompt,
                                   String userMessage, Double temperature, Integer maxTokens) {
        LlmServer server = findServerByIdAndUser(serverId, userId);
        LlmConnector connector = createConnector(server);
        return connector.chatStream(model, systemPrompt, userMessage, temperature, maxTokens);
    }

    public String generate(Long serverId, Long userId, String model, String prompt,
                           Double temperature, Integer maxTokens) {
        LlmServer server = findServerByIdAndUser(serverId, userId);
        LlmConnector connector = createConnector(server);
        return connector.generate(model, prompt, temperature, maxTokens);
    }

    public Flux<String> generateStream(Long serverId, Long userId, String model, String prompt,
                                        Double temperature, Integer maxTokens) {
        LlmServer server = findServerByIdAndUser(serverId, userId);
        LlmConnector connector = createConnector(server);
        return connector.generateStream(model, prompt, temperature, maxTokens);
    }

    public LlmConnector getConnector(Long serverId, Long userId) {
        LlmServer server = findServerByIdAndUser(serverId, userId);
        return createConnector(server);
    }

    private LlmServer findServerByIdAndUser(Long id, Long userId) {
        return llmServerRepository.findById(id)
                .filter(server -> server.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("LLM Server not found or access denied"));
    }

    private LlmConnector createConnector(LlmServer server) {
        return switch (server.getType()) {
            case OLLAMA -> new OllamaConnector(
                    server.getBaseUrl(),
                    server.getApiKey(),
                    ollamaNumCtx,
                    ollamaTimeout,
                    ollamaRetryCount
            );
            case VLLM, OPENAI_COMPATIBLE, CUSTOM ->
                new OpenAICompatibleConnector(server.getBaseUrl(), server.getApiKey());
        };
    }

    private String normalizeBaseUrl(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
