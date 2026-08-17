package com.loganalyzer.exception;

public class InvalidStatusTransitionException extends ConflictException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
