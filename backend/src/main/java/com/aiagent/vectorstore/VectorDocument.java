package com.aiagent.vectorstore;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDocument {

    private String id;
    private String content;
    private float[] embedding;
    private Map<String, Object> metadata;
}
