package com.aiagent.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @NotBlank(message = "Query is required")
    private String query;

    @Builder.Default
    private Integer topK = 5;

    private Map<String, Object> metadataFilter;
}
