package com.aiagent.service;

import com.aiagent.base.BaseServiceTest;
import com.aiagent.dto.agent.AgentRequest;
import com.aiagent.dto.agent.AgentResponse;
import com.aiagent.entity.Agent;
import com.aiagent.entity.User;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.FunctionRepository;
import com.aiagent.repository.KnowledgeBaseRepository;
import com.aiagent.repository.LlmServerRepository;
import com.aiagent.repository.UserRepository;
import com.aiagent.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AgentService Tests")
class AgentServiceTest extends BaseServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LlmServerRepository llmServerRepository;

    @Mock
    private FunctionRepository functionRepository;

    @Mock
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Mock
    private AgentVersionService agentVersionService;

    @InjectMocks
    private AgentService agentService;

    private User testUser;
    private Agent testAgent;

    @BeforeEach
    void setUp() {
        testUser = TestDataFactory.createUser();
        testUser.setId(1L);
        testAgent = TestDataFactory.createAgent(testUser);
        testAgent.setId(1L);
    }

    @Nested
    @DisplayName("findByUserId")
    class FindByUserId {

        @Test
        @DisplayName("Should return agents for user")
        void shouldReturnAgentsForUser() {
            // Given
            List<Agent> agents = List.of(testAgent);
            when(agentRepository.findByUserId(testUser.getId())).thenReturn(agents);

            // When
            List<AgentResponse> result = agentService.getAllAgents(testUser.getId());

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo(testAgent.getName());
            verify(agentRepository).findByUserId(testUser.getId());
        }

        @Test
        @DisplayName("Should return empty list when no agents")
        void shouldReturnEmptyListWhenNoAgents() {
            // Given
            when(agentRepository.findByUserId(testUser.getId())).thenReturn(List.of());

            // When
            List<AgentResponse> result = agentService.getAllAgents(testUser.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Should return agent when found")
        void shouldReturnAgentWhenFound() {
            // Given
            when(agentRepository.findById(testAgent.getId()))
                    .thenReturn(Optional.of(testAgent));

            // When
            AgentResponse result = agentService.getAgent(testAgent.getId(), testUser.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(testAgent.getName());
        }

        @Test
        @DisplayName("Should throw exception when agent not found")
        void shouldThrowExceptionWhenNotFound() {
            // Given
            when(agentRepository.findById(99L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> agentService.getAgent(99L, testUser.getId()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Agent not found");
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Should create new agent")
        void shouldCreateNewAgent() {
            // Given
            AgentRequest request = new AgentRequest();
            request.setName("New Agent");
            request.setDescription("Description");
            request.setSystemPrompt("System prompt");
            request.setModelName("gpt-4");
            request.setTemperature(0.7);
            request.setMaxTokens(2048);

            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(agentRepository.existsBySlug(any())).thenReturn(false);
            when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> {
                Agent saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });

            // When
            AgentResponse result = agentService.createAgent(request, testUser.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Agent");
            verify(agentRepository).save(any(Agent.class));
        }
    }
}
