package com.aiagent.dto.external;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * External API chat request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalChatRequest {

    @NotBlank(message = "Message is required")
    private String message;

    private String conversationId;

    private Boolean stream;
}
