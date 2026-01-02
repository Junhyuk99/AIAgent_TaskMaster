package com.aiagent.dto.conversation;

import com.aiagent.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private Long id;
    private String role;
    private String content;
    private Integer tokenCount;
    private String modelUsed;
    private String functionCalls;
    private String sources;
    private Long executionTimeMs;
    private LocalDateTime createdAt;

    public static MessageResponse from(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .role(message.getRole().name().toLowerCase())
                .content(message.getContent())
                .tokenCount(message.getTokenCount())
                .modelUsed(message.getModelUsed())
                .functionCalls(message.getFunctionCalls())
                .sources(message.getSources())
                .executionTimeMs(message.getExecutionTimeMs())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
