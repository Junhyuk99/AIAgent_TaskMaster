package com.aiagent.dto.function;

import com.aiagent.entity.Function;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionResponse {

    private Long id;
    private String name;
    private String description;
    private String parametersSchema;
    private String returnType;
    private Function.ImplementationType implementationType;
    private String implementationConfig;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FunctionResponse from(Function function) {
        return FunctionResponse.builder()
                .id(function.getId())
                .name(function.getName())
                .description(function.getDescription())
                .parametersSchema(function.getParametersSchema())
                .returnType(function.getReturnType())
                .implementationType(function.getImplementationType())
                .implementationConfig(function.getImplementationConfig())
                .isActive(function.getIsActive())
                .createdAt(function.getCreatedAt())
                .updatedAt(function.getUpdatedAt())
                .build();
    }
}
