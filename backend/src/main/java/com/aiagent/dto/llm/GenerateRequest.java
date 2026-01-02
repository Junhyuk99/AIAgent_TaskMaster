package com.aiagent.dto.llm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequest {

    @NotNull(message = "Server ID is required")
    private Long serverId;

    @NotBlank(message = "Model is required")
    private String model;

    @NotBlank(message = "Prompt is required")
    private String prompt;

    @Builder.Default
    private Double temperature = 0.7;

    @Builder.Default
    private Integer maxTokens = 2048;
}
