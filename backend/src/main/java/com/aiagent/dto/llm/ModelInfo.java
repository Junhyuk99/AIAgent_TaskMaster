package com.aiagent.dto.llm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelInfo {

    private String name;
    private String displayName;
    private Long size;
    private String modifiedAt;
}
