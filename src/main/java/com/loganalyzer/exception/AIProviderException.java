package com.loganalyzer.exception;

public class AIProviderException extends RuntimeException {

    private final boolean retryable;
    private final Integer providerStatus;

    public AIProviderException(
            String message,
            boolean retryable,
            Integer providerStatus
    ) {
        super(message);
        this.retryable = retryable;
        this.providerStatus = providerStatus;
    }

    public AIProviderException(
            String message,
            Throwable cause,
            boolean retryable,
            Integer providerStatus
    ) {
        super(message, cause);
        this.retryable = retryable;
        this.providerStatus = providerStatus;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer getProviderStatus() {
        return providerStatus;
    }
}
