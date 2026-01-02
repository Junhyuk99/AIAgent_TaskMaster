package com.aiagent.service;

import com.aiagent.dto.conversation.ConversationListResponse;
import com.aiagent.dto.conversation.ConversationResponse;
import com.aiagent.dto.conversation.MessageResponse;
import com.aiagent.entity.Agent;
import com.aiagent.entity.Conversation;
import com.aiagent.entity.Message;
import com.aiagent.entity.User;
import com.aiagent.repository.AgentRepository;
import com.aiagent.repository.ConversationRepository;
import com.aiagent.repository.MessageRepository;
import com.aiagent.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final AgentRepository agentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Get all conversations for an agent.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsByAgent(Long agentId, Long userId) {
        return conversationRepository.findByAgentIdAndUserId(agentId, userId).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    /**
     * Get paginated conversations for an agent.
     */
    @Transactional(readOnly = true)
    public ConversationListResponse getConversationsByAgentPaged(Long agentId, Long userId, int page, int size) {
        Page<Conversation> conversations = conversationRepository.findByAgentIdAndUserIdPaged(
                agentId, userId, PageRequest.of(page, size));

        return ConversationListResponse.builder()
                .conversations(conversations.getContent().stream().map(ConversationResponse::from).toList())
                .page(page)
                .size(size)
                .totalElements(conversations.getTotalElements())
                .totalPages(conversations.getTotalPages())
                .build();
    }

    /**
     * Get all conversations for a user.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsByUser(Long userId) {
        return conversationRepository.findByUserId(userId).stream()
                .map(ConversationResponse::from)
                .toList();
    }

    /**
     * Get a conversation by external ID with all messages.
     */
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(String externalId, Long userId) {
        Conversation conversation = conversationRepository.findByExternalIdWithMessages(externalId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return ConversationResponse.fromWithMessages(conversation);
    }

    /**
     * Create a new conversation.
     */
    @Transactional
    public Conversation createConversation(Long agentId, Long userId, String title) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation = Conversation.builder()
                .externalId(UUID.randomUUID().toString())
                .title(title != null ? title : "New Conversation")
                .agent(agent)
                .user(user)
                .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Get or create a conversation by external ID.
     */
    @Transactional
    public Conversation getOrCreateConversation(String externalId, Long agentId, Long userId) {
        if (externalId != null && !externalId.isEmpty()) {
            return conversationRepository.findByExternalIdAndUserId(externalId, userId)
                    .orElseGet(() -> createConversationWithExternalId(externalId, agentId, userId));
        }
        return createConversation(agentId, userId, null);
    }

    private Conversation createConversationWithExternalId(String externalId, Long agentId, Long userId) {
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Conversation conversation = Conversation.builder()
                .externalId(externalId)
                .title("New Conversation")
                .agent(agent)
                .user(user)
                .build();

        return conversationRepository.save(conversation);
    }

    /**
     * Add a message to a conversation.
     */
    @Transactional
    public Message addMessage(Conversation conversation, Message.Role role, String content,
                              String modelUsed, Long executionTimeMs, Object functionCalls, Object sources) {
        Message message = Message.builder()
                .role(role)
                .content(content)
                .conversation(conversation)
                .modelUsed(modelUsed)
                .executionTimeMs(executionTimeMs)
                .functionCalls(serializeJson(functionCalls))
                .sources(serializeJson(sources))
                .build();

        conversation.addMessage(message);
        messageRepository.save(message);
        conversationRepository.save(conversation);

        return message;
    }

    /**
     * Update conversation title.
     */
    @Transactional
    public ConversationResponse updateTitle(String externalId, Long userId, String title) {
        Conversation conversation = conversationRepository.findByExternalIdAndUserId(externalId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversation.setTitle(title);
        conversationRepository.save(conversation);

        return ConversationResponse.from(conversation);
    }

    /**
     * Archive a conversation.
     */
    @Transactional
    public void archiveConversation(String externalId, Long userId) {
        Conversation conversation = conversationRepository.findByExternalIdAndUserId(externalId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversation.setIsArchived(true);
        conversationRepository.save(conversation);
        log.info("Archived conversation: {}", externalId);
    }

    /**
     * Delete a conversation.
     */
    @Transactional
    public void deleteConversation(String externalId, Long userId) {
        Conversation conversation = conversationRepository.findByExternalIdAndUserId(externalId, userId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        conversationRepository.delete(conversation);
        log.info("Deleted conversation: {}", externalId);
    }

    /**
     * Export conversation as JSON.
     */
    @Transactional(readOnly = true)
    public String exportConversation(String externalId, Long userId) {
        ConversationResponse conversation = getConversation(externalId, userId);
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(conversation);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to export conversation", e);
        }
    }

    /**
     * Generate conversation title from first message.
     */
    public String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isEmpty()) {
            return "New Conversation";
        }
        // Truncate to 50 chars and clean up
        String title = firstMessage.replaceAll("\\s+", " ").trim();
        if (title.length() > 50) {
            title = title.substring(0, 47) + "...";
        }
        return title;
    }

    private String serializeJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize object to JSON", e);
            return null;
        }
    }
}
