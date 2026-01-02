package com.aiagent.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagContext {

    private String originalSystemPrompt;

    private String augmentedSystemPrompt;

    private String retrievedContext;

    private List<RagSearchResponse.DocumentSource> sources;

    private boolean contextFound;
}
