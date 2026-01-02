package com.aiagent.dto.llm;

import com.aiagent.entity.LlmServer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmServerRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @NotNull(message = "Type is required")
    private LlmServer.LlmType type;

    @NotBlank(message = "Base URL is required")
    @Size(max = 500, message = "Base URL must not exceed 500 characters")
    @Pattern(regexp = "^https?://.*", message = "Base URL must start with http:// or https://")
    private String baseUrl;

    @Size(max = 500, message = "API key must not exceed 500 characters")
    private String apiKey;
}
