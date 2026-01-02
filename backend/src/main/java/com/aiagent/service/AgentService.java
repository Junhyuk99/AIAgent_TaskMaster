package com.aiagent.service;

import com.aiagent.dto.agent.AgentRequest;
import com.aiagent.dto.agent.AgentResponse;
import com.aiagent.entity.Agent;
import com.aiagent.entity.Function;
import com.aiagent.entity.KnowledgeBase;
import com.aiagent.entity.LlmServer;
import com.aiagent.entity.User;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.FunctionRepository;
import com.aiagent.repository.KnowledgeBaseRepository;
import com.aiagent.repository.LlmServerRepository;
import com.aiagent.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final LlmServerRepository llmServerRepository;
    private final FunctionRepository functionRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final AgentVersionService agentVersionService;

    @Transactional(readOnly = true)
    public List<AgentResponse> getAllAgents(Long userId) {
        return agentRepository.findByUserId(userId).stream()
                .map(AgentResponse::fromBasic)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AgentResponse> getAgentsPaged(Long userId, Pageable pageable) {
        return agentRepository.findByUserId(userId, pageable)
                .map(AgentResponse::fromBasic);
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> getActiveAgents(Long userId) {
        return agentRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(AgentResponse::fromBasic)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentResponse getAgent(Long id, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);
        return AgentResponse.from(agent);
    }

    @Transactional
    public AgentResponse createAgent(AgentRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Agent agent = Agent.builder()
                .name(request.getName())
                .slug(generateUniqueSlug(request.getName()))
                .description(request.getDescription())
                .systemPrompt(request.getSystemPrompt())
                .modelName(request.getModelName())
                .temperature(request.getTemperature())
                .maxTokens(request.getMaxTokens())
                .isActive(true)
                .user(user)
                .build();

        if (request.getLlmServerId() != null) {
            LlmServer llmServer = llmServerRepository.findById(request.getLlmServerId())
                    .filter(server -> server.getUser().getId().equals(userId))
                    .orElseThrow(() -> new RuntimeException("LLM Server not found or access denied"));
            agent.setLlmServer(llmServer);
        }

        if (request.getFunctionIds() != null && !request.getFunctionIds().isEmpty()) {
            Set<Function> functions = new HashSet<>();
            for (Long funcId : request.getFunctionIds()) {
                Function function = functionRepository.findById(funcId)
                        .filter(f -> f.getUser().getId().equals(userId))
                        .orElseThrow(() -> new RuntimeException("Function not found: " + funcId));
                functions.add(function);
            }
            agent.setFunctions(functions);
        }

        if (request.getKnowledgeBaseIds() != null && !request.getKnowledgeBaseIds().isEmpty()) {
            Set<KnowledgeBase> knowledgeBases = new HashSet<>();
            for (Long kbId : request.getKnowledgeBaseIds()) {
                KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                        .filter(k -> k.getUser().getId().equals(userId))
                        .orElseThrow(() -> new RuntimeException("Knowledge Base not found: " + kbId));
                knowledgeBases.add(kb);
            }
            agent.setKnowledgeBases(knowledgeBases);
        }

        Agent saved = agentRepository.save(agent);
        log.info("Created agent: {} for user: {}", saved.getName(), userId);
        return AgentResponse.from(saved);
    }

    @Transactional
    public AgentResponse updateAgent(Long id, AgentRequest request, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);

        // Create version snapshot before updating
        agentVersionService.createVersion(agent, agent.getUser(), "Agent settings updated");

        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setSystemPrompt(request.getSystemPrompt());
        agent.setModelName(request.getModelName());
        agent.setTemperature(request.getTemperature());
        agent.setMaxTokens(request.getMaxTokens());

        if (request.getLlmServerId() != null) {
            LlmServer llmServer = llmServerRepository.findById(request.getLlmServerId())
                    .filter(server -> server.getUser().getId().equals(userId))
                    .orElseThrow(() -> new RuntimeException("LLM Server not found or access denied"));
            agent.setLlmServer(llmServer);
        } else {
            agent.setLlmServer(null);
        }

        if (request.getFunctionIds() != null) {
            Set<Function> functions = new HashSet<>();
            for (Long funcId : request.getFunctionIds()) {
                Function function = functionRepository.findById(funcId)
                        .filter(f -> f.getUser().getId().equals(userId))
                        .orElseThrow(() -> new RuntimeException("Function not found: " + funcId));
                functions.add(function);
            }
            agent.setFunctions(functions);
        }

        if (request.getKnowledgeBaseIds() != null) {
            Set<KnowledgeBase> knowledgeBases = new HashSet<>();
            for (Long kbId : request.getKnowledgeBaseIds()) {
                KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                        .filter(k -> k.getUser().getId().equals(userId))
                        .orElseThrow(() -> new RuntimeException("Knowledge Base not found: " + kbId));
                knowledgeBases.add(kb);
            }
            agent.setKnowledgeBases(knowledgeBases);
        }

        Agent updated = agentRepository.save(agent);
        log.info("Updated agent: {}", updated.getName());
        return AgentResponse.from(updated);
    }

    @Transactional
    public void deleteAgent(Long id, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);
        agentRepository.delete(agent);
        log.info("Deleted agent: {}", agent.getName());
    }

    @Transactional
    public AgentResponse toggleActive(Long id, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);
        agent.setIsActive(!agent.getIsActive());
        Agent updated = agentRepository.save(agent);
        log.info("Toggled agent active status: {} -> {}", agent.getName(), updated.getIsActive());
        return AgentResponse.from(updated);
    }

    @Transactional
    public AgentResponse duplicateAgent(Long id, Long userId) {
        Agent original = findAgentByIdAndUser(id, userId);

        Agent duplicate = Agent.builder()
                .name(original.getName() + " (Copy)")
                .description(original.getDescription())
                .systemPrompt(original.getSystemPrompt())
                .modelName(original.getModelName())
                .temperature(original.getTemperature())
                .maxTokens(original.getMaxTokens())
                .isActive(true)
                .user(original.getUser())
                .llmServer(original.getLlmServer())
                .functions(new HashSet<>(original.getFunctions()))
                .knowledgeBases(new HashSet<>(original.getKnowledgeBases()))
                .build();

        Agent saved = agentRepository.save(duplicate);
        log.info("Duplicated agent: {} -> {}", original.getName(), saved.getName());
        return AgentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AgentResponse> searchAgents(Long userId, String query) {
        return agentRepository.findByUserId(userId).stream()
                .filter(agent -> agent.getName().toLowerCase().contains(query.toLowerCase()) ||
                        (agent.getDescription() != null &&
                         agent.getDescription().toLowerCase().contains(query.toLowerCase())))
                .map(AgentResponse::fromBasic)
                .toList();
    }

    private Agent findAgentByIdAndUser(Long id, Long userId) {
        return agentRepository.findById(id)
                .filter(agent -> agent.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Agent not found or access denied"));
    }

    /**
     * Generate or update the slug for an agent.
     */
    @Transactional
    public AgentResponse generateSlug(Long id, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);
        agent.setSlug(generateUniqueSlug(agent.getName()));
        Agent updated = agentRepository.save(agent);
        log.info("Generated slug for agent: {} -> {}", agent.getName(), updated.getSlug());
        return AgentResponse.from(updated);
    }

    /**
     * Update the functions connected to an agent.
     */
    @Transactional
    public AgentResponse updateFunctions(Long id, List<Long> functionIds, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);

        Set<Function> functions = new HashSet<>();
        if (functionIds != null) {
            for (Long funcId : functionIds) {
                Function function = functionRepository.findById(funcId)
                        .filter(f -> f.getUser().getId().equals(userId))
                        .orElseThrow(() -> new RuntimeException("Function not found: " + funcId));
                functions.add(function);
            }
        }
        agent.setFunctions(functions);

        Agent updated = agentRepository.save(agent);
        log.info("Updated functions for agent: {} (count: {})", agent.getName(), functions.size());
        return AgentResponse.from(updated);
    }

    /**
     * Update the knowledge bases connected to an agent.
     */
    @Transactional
    public AgentResponse updateKnowledgeBases(Long id, List<Long> knowledgeBaseIds, Long userId) {
        Agent agent = findAgentByIdAndUser(id, userId);

        Set<KnowledgeBase> knowledgeBases = new HashSet<>();
        if (knowledgeBaseIds != null) {
            for (Long kbId : knowledgeBaseIds) {
                KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
                        .filter(k -> k.getUser().getId().equals(userId))
                        .orElseThrow(() -> new RuntimeException("Knowledge Base not found: " + kbId));
                knowledgeBases.add(kb);
            }
        }
        agent.setKnowledgeBases(knowledgeBases);

        Agent updated = agentRepository.save(agent);
        log.info("Updated knowledge bases for agent: {} (count: {})", agent.getName(), knowledgeBases.size());
        return AgentResponse.from(updated);
    }

    /**
     * Generate a unique slug from the given name.
     */
    private String generateUniqueSlug(String name) {
        String baseSlug = toSlug(name);
        String slug = baseSlug;
        int counter = 1;

        while (agentRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }

    /**
     * Convert a string to a URL-safe slug.
     */
    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    private String toSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "agent-" + UUID.randomUUID().toString().substring(0, 8);
        }

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        slug = slug.toLowerCase(Locale.ENGLISH).replaceAll("-+", "-");
        slug = slug.replaceAll("^-|-$", "");

        if (slug.isEmpty()) {
            return "agent-" + UUID.randomUUID().toString().substring(0, 8);
        }

        return slug;
    }
}
