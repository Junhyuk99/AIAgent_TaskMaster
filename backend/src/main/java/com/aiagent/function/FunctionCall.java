package com.aiagent.function;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Represents a function call request from the LLM.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCall {

    /**
     * The name of the function to call.
     */
    private String name;

    /**
     * The arguments to pass to the function, parsed from JSON.
     */
    private Map<String, Object> arguments;

    /**
     * Optional ID for tracking (used by some LLM APIs).
     */
    private String id;
}
