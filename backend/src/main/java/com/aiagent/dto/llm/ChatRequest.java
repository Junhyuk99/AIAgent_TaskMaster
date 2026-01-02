package com.aiagent.dto.llm;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotNull(message = "Server ID is required")
    private Long serverId;

    @NotBlank(message = "Model is required")
    @Size(max = 100, message = "Model name must not exceed 100 characters")
    private String model;

    @Size(max = 10000, message = "System prompt must not exceed 10000 characters")
    private String systemPrompt;

    @NotBlank(message = "User message is required")
    @Size(max = 100000, message = "User message must not exceed 100000 characters")
    private String userMessage;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Temperature must be at least 0")
    @DecimalMax(value = "2.0", message = "Temperature must not exceed 2")
    private Double temperature = 0.7;

    @Builder.Default
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 32768, message = "Max tokens must not exceed 32768")
    private Integer maxTokens = 2048;
}
