package com.aiagent.repository;

import com.aiagent.base.BaseRepositoryTest;
import com.aiagent.entity.Agent;
import com.aiagent.entity.User;
import com.aiagent.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgentRepository Tests")
class AgentRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(TestDataFactory.createUser());
    }

    @Test
    @DisplayName("Should save and find agent by user")
    void shouldFindAgentsByUser() {
        // Given
        Agent agent1 = TestDataFactory.createAgent(testUser, "Agent 1");
        Agent agent2 = TestDataFactory.createAgent(testUser, "Agent 2");
        agentRepository.save(agent1);
        agentRepository.save(agent2);

        // When
        List<Agent> agents = agentRepository.findByUserId(testUser.getId());

        // Then
        assertThat(agents).hasSize(2);
    }

    @Test
    @DisplayName("Should find agent by slug")
    void shouldFindAgentBySlug() {
        // Given
        Agent agent = TestDataFactory.createAgent(testUser, "Test Agent");
        agentRepository.save(agent);

        // When
        Optional<Agent> found = agentRepository.findBySlug(agent.getSlug());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Test Agent");
    }

    @Test
    @DisplayName("Should find active agents by user")
    void shouldFindActiveAgentsByUser() {
        // Given
        Agent activeAgent = TestDataFactory.createAgent(testUser, "Active Agent");
        activeAgent.setIsActive(true);

        Agent inactiveAgent = TestDataFactory.createAgent(testUser, "Inactive Agent");
        inactiveAgent.setIsActive(false);

        agentRepository.save(activeAgent);
        agentRepository.save(inactiveAgent);

        // When
        List<Agent> activeAgents = agentRepository.findByUserIdAndIsActiveTrue(testUser.getId());

        // Then
        assertThat(activeAgents).hasSize(1);
        assertThat(activeAgents.get(0).getName()).isEqualTo("Active Agent");
    }
}
