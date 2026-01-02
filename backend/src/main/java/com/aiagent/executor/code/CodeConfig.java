package com.aiagent.executor.code;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for code-based function execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeConfig {

    /**
     * The code to execute.
     */
    private String code;

    /**
     * The programming language (javascript, python, etc.).
     */
    @Builder.Default
    private String language = "javascript";

    /**
     * Maximum execution time in milliseconds.
     */
    @Builder.Default
    private int timeoutMs = 5000;

    /**
     * Maximum memory usage in MB.
     */
    @Builder.Default
    private int maxMemoryMb = 128;

    /**
     * Whether to allow network access.
     */
    @Builder.Default
    private boolean allowNetwork = false;

    /**
     * Whether to allow file system access.
     */
    @Builder.Default
    private boolean allowFileSystem = false;
}
