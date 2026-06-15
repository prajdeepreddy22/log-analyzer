package com.loganalyzer.exception;

public class InsufficientStorageException extends ApiException {

    public InsufficientStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
