package com.aiagent.dto.conversation;

import com.aiagent.entity.Conversation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;
    private String externalId;
    private String title;
    private String summary;
    private Long agentId;
    private String agentName;
    private Integer messageCount;
    private Boolean isArchived;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<MessageResponse> messages;

    public static ConversationResponse from(Conversation conversation) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .externalId(conversation.getExternalId())
                .title(conversation.getTitle())
                .summary(conversation.getSummary())
                .agentId(conversation.getAgent().getId())
                .agentName(conversation.getAgent().getName())
                .messageCount(conversation.getMessageCount())
                .isArchived(conversation.getIsArchived())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }

    public static ConversationResponse fromWithMessages(Conversation conversation) {
        ConversationResponse response = from(conversation);
        if (conversation.getMessages() != null) {
            response.setMessages(
                    conversation.getMessages().stream()
                            .map(MessageResponse::from)
                            .toList()
            );
        }
        return response;
    }
}
