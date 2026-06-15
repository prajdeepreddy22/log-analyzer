package com.loganalyzer.exception;

import com.loganalyzer.storage.StorageException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(
            ResourceNotFoundException ex) {

        log.warn(
                "Resource not found type={}",
                ex.getClass().getSimpleName()
        );
        return build(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex) {

        log.warn(
                "Unauthorized request type={}",
                ex.getClass().getSimpleName()
        );
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized", ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex) {

        log.warn(
                "Authentication failed type={}",
                ex.getClass().getSimpleName()
        );
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
    public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {

        log.warn(
                "Bad request type={}",
                ex.getClass().getSimpleName()
        );
        return build(HttpStatus.BAD_REQUEST, "Bad request", ex.getMessage());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(Exception ex) {

        log.warn(
                "Malformed request type={}",
                ex.getClass().getSimpleName()
        );
        return build(
                HttpStatus.BAD_REQUEST,
                "Bad request",
                malformedRequestDetails(ex)
        );
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex) {

        log.warn("Unsupported media type: {}", ex.getContentType());
        return build(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type",
                "The request content type is not supported"
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMethod(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Unsupported method: {}", ex.getMethod());
        return build(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method not allowed",
                "The HTTP method is not supported for this endpoint"
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex) {

        log.warn(
                "Data integrity conflict type={}",
                ex.getClass().getSimpleName()
        );
        return build(
                HttpStatus.CONFLICT,
                "Conflict",
                "The request conflicts with existing data"
        );
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(
            ConflictException ex) {

        log.warn(
                "Request conflict type={}",
                ex.getClass().getSimpleName()
        );
        return build(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(InsufficientStorageException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientStorage(
            InsufficientStorageException ex) {

        log.error(
                "Insufficient file storage type={}",
                ex.getClass().getSimpleName()
        );
        return build(
                HttpStatus.INSUFFICIENT_STORAGE,
                "Insufficient storage",
                "The server does not have enough storage for this upload"
        );
    }

    @ExceptionHandler({
            StorageException.class,
            ServiceUnavailableException.class
    })
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(
            RuntimeException ex) {

        log.error(
                "Required service unavailable type={}",
                ex.getClass().getSimpleName()
        );
        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service unavailable",
                "A required service is temporarily unavailable"
        );
    }

    @ExceptionHandler(AIProviderException.class)
    public ResponseEntity<Map<String, Object>> handleAIProvider(
            AIProviderException ex) {

        HttpStatus status = ex.isRetryable()
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.BAD_GATEWAY;

        log.error(
                "AI provider failure retryable={} providerStatus={} type={}",
                ex.isRetryable(),
                ex.getProviderStatus(),
                ex.getClass().getSimpleName()
        );

        return build(
                status,
                "AI service error",
                ex.isRetryable()
                        ? "AI service is temporarily unavailable"
                        : "AI service could not process the request"
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(
            DataAccessException ex) {

        log.error(
                "Database operation failed type={}",
                ex.getClass().getSimpleName()
        );
        return build(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service unavailable",
                "The database is temporarily unavailable"
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex,
            HttpServletRequest request) {

        log.warn(
                "Rate limit exceeded path={} message={}",
                request.getRequestURI(),
                ex.getMessage()
        );

        return build(
                HttpStatus.TOO_MANY_REQUESTS,
                "Too Many Requests",
                ex.getMessage(),
                request.getRequestURI()
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

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class
    })
    public ResponseEntity<Map<String, Object>> handleValidation(Exception ex) {

        String errors;

        if (ex instanceof MethodArgumentNotValidException validationException) {
            errors = fieldErrors(validationException.getBindingResult()
                    .getFieldErrors());
        } else {
            errors = fieldErrors(((BindException) ex).getBindingResult()
                    .getFieldErrors());
        }

        return build(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                errors.isBlank() ? "Request validation failed" : errors
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {

        String errorId = UUID.randomUUID().toString();
        log.error(
                "Unexpected error errorId={} type={}",
                errorId,
                ex.getClass().getSimpleName()
        );

        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "An unexpected error occurred. Reference: " + errorId
        );
    }

    private String malformedRequestDetails(Exception ex) {

        if (ex instanceof MissingServletRequestParameterException missing) {
            return "Missing required parameter: " + missing.getParameterName();
        }

        if (ex instanceof MissingServletRequestPartException missing) {
            return "Missing required request part: " + missing.getRequestPartName();
        }

        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Invalid value for parameter: " + mismatch.getName();
        }

        if (ex instanceof ConstraintViolationException violation) {
            return violation.getConstraintViolations().stream()
                    .map(item -> item.getPropertyPath() + ": " + item.getMessage())
                    .collect(Collectors.joining(", "));
        }

        return "The request body is missing or malformed";
    }

    private String fieldErrors(java.util.List<FieldError> errors) {
        return errors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status,
            String error,
            String details) {

        return build(status, error, details, null);
    }

    private ResponseEntity<Map<String, Object>> build(
            HttpStatus status,
            String error,
            String details,
            String path) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("details", details != null ? details : "No details");

        if (path != null) {
            body.put("path", path);
        }

        return new ResponseEntity<>(body, status);
    }
}
