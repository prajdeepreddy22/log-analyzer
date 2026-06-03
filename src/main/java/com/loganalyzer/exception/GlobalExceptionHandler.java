package com.loganalyzer.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {

        log.warn("Resource not found: {}", ex.getMessage());

        return build(
                HttpStatus.NOT_FOUND,
                "Resource not found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex) {

        log.warn("Unauthorized: {}", ex.getMessage());

        return build(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                ex.getMessage()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex) {

        log.warn("Authentication failed: {}", ex.getMessage());

        return build(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                "Invalid username or password"
        );
    }

    @ExceptionHandler({
            BadRequestException.class,
            FileValidationException.class
    })
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            Exception ex) {

        log.warn("Bad request: {}", ex.getMessage());

        return build(
                HttpStatus.BAD_REQUEST,
                "Bad request",
                ex.getMessage()
        );
    }

    // =====================================================
    // RATE LIMIT EXCEPTION
    // =====================================================

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn(
                "Rate limit exceeded path={} message={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("details", ex.getMessage());
        body.put("path", request.getRequestURI());

        return new ResponseEntity<>(
                body,
                HttpStatus.TOO_MANY_REQUESTS
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSize() {

        return build(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "File too large",
                "Max allowed size exceeded"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(
            Exception ex) {

        log.error("Unexpected error", ex);

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                ex.getMessage()
        );
    }

    // =====================================================
    // COMMON RESPONSE BUILDER
    // =====================================================

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status,
            String error,
            String details) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put(
                "details",
                details != null ? details : "No details"
        );

        return new ResponseEntity<>(body, status);
    }
}
