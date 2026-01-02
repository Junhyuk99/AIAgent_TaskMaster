package com.aiagent.executor;

/**
 * Exception thrown when function execution fails.
 */
public class FunctionExecutionException extends Exception {

    private final String errorCode;

    public FunctionExecutionException(String message) {
        super(message);
        this.errorCode = "EXECUTION_ERROR";
    }

    public FunctionExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "EXECUTION_ERROR";
    }

    public FunctionExecutionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public FunctionExecutionException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
