package com.aiagent.executor.template;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for template-based function execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateConfig {

    /**
     * The template string with {{paramName}} placeholders.
     */
    private String template;

    /**
     * Output format for the result.
     */
    @Builder.Default
    private OutputFormat outputFormat = OutputFormat.WRAPPED;

    /**
     * Output format options.
     */
    public enum OutputFormat {
        TEXT,    // Return as plain text
        JSON,    // Parse result as JSON
        WRAPPED  // Wrap in {result: "..."}
    }
}
