package com.aiagent.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

    private String content;
    private int index;
    private int startOffset;
    private int endOffset;
    private Map<String, Object> metadata;

    public int getLength() {
        return content != null ? content.length() : 0;
    }
}
