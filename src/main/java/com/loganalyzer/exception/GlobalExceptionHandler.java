package com.loganalyzer.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // =========================
    // NOT FOUND
    // =========================
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage()
        );
    }

    // =========================
    // UNAUTHORIZED
    // =========================
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(UnauthorizedException ex) {

        log.warn("Unauthorized access: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Unauthorized",
                ex.getMessage()
        );
    }

    // =========================
    // BAD REQUEST
    // =========================
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(BadRequestException ex) {

        log.warn("Bad request: {}", ex.getMessage());

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Bad request",
                ex.getMessage()
        );
    }

    // =========================
    // FILE SIZE
    // =========================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSize(MaxUploadSizeExceededException ex) {

        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File too large",
                "File size exceeds maximum limit (10MB)"
        );
    }

    // =========================
    // VALIDATION
    // =========================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError ->
                        fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                errors
        );
    }

    // =========================
    // GENERIC
    // =========================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {

        log.error("Unexpected error occurred", ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                ex.getMessage() != null ? ex.getMessage() : "Something went wrong"
        );
    }

    // =========================
    // COMMON RESPONSE BUILDER
    // =========================
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String error,
            String details) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);      // short message
        body.put("details", details);  // actual reason

        return new ResponseEntity<>(body, status);
    }
}