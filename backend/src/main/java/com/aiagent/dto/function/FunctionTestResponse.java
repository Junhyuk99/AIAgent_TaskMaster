package com.aiagent.dto.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionTestResponse {
    private boolean success;
    private Object result;
    private String error;
    private Long executionTimeMs;
}
