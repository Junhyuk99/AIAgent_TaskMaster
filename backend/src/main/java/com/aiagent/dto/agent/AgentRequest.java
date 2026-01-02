package com.aiagent.dto.agent;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 10000, message = "System prompt must not exceed 10000 characters")
    private String systemPrompt;

    private Long llmServerId;

    @Size(max = 100, message = "Model name must not exceed 100 characters")
    private String modelName;

    @Builder.Default
    @DecimalMin(value = "0.0", message = "Temperature must be at least 0")
    @DecimalMax(value = "2.0", message = "Temperature must not exceed 2")
    private Double temperature = 0.7;

    @Builder.Default
    @Min(value = 1, message = "Max tokens must be at least 1")
    @Max(value = 32768, message = "Max tokens must not exceed 32768")
    private Integer maxTokens = 2048;

    @Size(max = 50, message = "Cannot assign more than 50 functions")
    private Set<Long> functionIds;

    @Size(max = 20, message = "Cannot assign more than 20 knowledge bases")
    private Set<Long> knowledgeBaseIds;
}
