package com.aiagent.service;

import com.aiagent.dto.AgentVersionResponse;
import com.aiagent.entity.Agent;
import com.aiagent.entity.AgentVersion;
import com.aiagent.entity.User;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.AgentVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentVersionService {

    private final AgentVersionRepository agentVersionRepository;
    private final AgentRepository agentRepository;

    @Transactional
    public AgentVersion createVersion(Agent agent, User createdBy, String changeSummary) {
        // Get next version number
        Integer nextVersion = agentVersionRepository.findMaxVersionNumber(agent.getId())
                .map(v -> v + 1)
                .orElse(1);

        // Clear current version flag from existing versions
        agentVersionRepository.clearCurrentVersion(agent.getId());

        // Create snapshot of current agent state
        AgentVersion version = AgentVersion.builder()
                .agent(agent)
                .versionNumber(nextVersion)
                .name(agent.getName())
                .description(agent.getDescription())
                .systemPrompt(agent.getSystemPrompt())
                .modelName(agent.getModelName())
                .temperature(agent.getTemperature())
                .maxTokens(agent.getMaxTokens())
                .llmServerId(agent.getLlmServer() != null ? agent.getLlmServer().getId() : null)
                .functionIds(formatIds(agent.getFunctions().stream()
                        .map(f -> f.getId())
                        .collect(Collectors.toList())))
                .knowledgeBaseIds(formatIds(agent.getKnowledgeBases().stream()
                        .map(kb -> kb.getId())
                        .collect(Collectors.toList())))
                .changeSummary(changeSummary)
                .createdBy(createdBy)
                .isCurrent(true)
                .build();

        log.info("Creating version {} for agent {}", nextVersion, agent.getId());
        return agentVersionRepository.save(version);
    }

    @Transactional(readOnly = true)
    public List<AgentVersionResponse> getVersionHistory(Long agentId, Long userId) {
        // Verify user has access to agent
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (!agent.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return agentVersionRepository.findByAgentIdOrderByVersionNumberDesc(agentId)
                .stream()
                .map(AgentVersionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<AgentVersionResponse> getVersionHistoryPaged(Long agentId, Long userId, Pageable pageable) {
        // Verify user has access to agent
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (!agent.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return agentVersionRepository.findByAgentIdOrderByVersionNumberDesc(agentId, pageable)
                .map(AgentVersionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public AgentVersionResponse getVersion(Long agentId, Integer versionNumber, Long userId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (!agent.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        AgentVersion version = agentVersionRepository.findByAgentIdAndVersionNumber(agentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        return AgentVersionResponse.fromEntity(version);
    }

    @Transactional
    public Agent rollbackToVersion(Long agentId, Integer versionNumber, Long userId) {
        Agent agent = agentRepository.findByIdWithAllRelations(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));

        if (!agent.getUser().getId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        AgentVersion version = agentVersionRepository.findByAgentIdAndVersionNumber(agentId, versionNumber)
                .orElseThrow(() -> new RuntimeException("Version not found"));

        // Create a new version to record the rollback before applying
        User user = agent.getUser();

        // Apply version data to agent
        agent.setName(version.getName());
        agent.setDescription(version.getDescription());
        agent.setSystemPrompt(version.getSystemPrompt());
        agent.setModelName(version.getModelName());
        agent.setTemperature(version.getTemperature());
        agent.setMaxTokens(version.getMaxTokens());

        // Note: LLM server and functions/knowledge bases need to be handled separately
        // as they require loading from their repositories

        Agent savedAgent = agentRepository.save(agent);

        // Create a new version to record the rollback
        createVersion(savedAgent, user, "Rollback to version " + versionNumber);

        log.info("Agent {} rolled back to version {}", agentId, versionNumber);
        return savedAgent;
    }

    @Transactional(readOnly = true)
    public AgentVersionResponse compareVersions(Long agentId, Integer version1, Integer version2, Long userId) {
        // For simplicity, return the newer version with comparison data
        // In a full implementation, you might return a diff object
        return getVersion(agentId, version2, userId);
    }

    @Transactional(readOnly = true)
    public long getVersionCount(Long agentId) {
        return agentVersionRepository.countByAgentId(agentId);
    }

    private String formatIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
