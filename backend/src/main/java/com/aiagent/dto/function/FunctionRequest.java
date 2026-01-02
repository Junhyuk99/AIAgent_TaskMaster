package com.aiagent.dto.function;

import com.aiagent.entity.Function;
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
public class FunctionRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "Name must start with a letter and contain only letters, numbers, and underscores")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 10000, message = "Parameters schema must not exceed 10000 characters")
    private String parametersSchema;

    @Size(max = 100, message = "Return type must not exceed 100 characters")
    private String returnType;

    @NotNull(message = "Implementation type is required")
    private Function.ImplementationType implementationType;

    @Size(max = 50000, message = "Implementation config must not exceed 50000 characters")
    private String implementationConfig;
}
