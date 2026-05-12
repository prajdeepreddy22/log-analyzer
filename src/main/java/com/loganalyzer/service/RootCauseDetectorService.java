package com.loganalyzer.service;

import com.loganalyzer.dto.response.RootCauseResult;
import com.loganalyzer.entity.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class RootCauseDetectorService {

    // =====================================================
    // MAIN ROOT CAUSE DETECTION
    // =====================================================
    public RootCauseResult detectRootCause(
            List<Log> logs
    ) {

        if (logs == null || logs.isEmpty()) {

            return RootCauseResult.builder()
                    .rootCause("UNKNOWN")
                    .confidence(0)
                    .build();
        }

        log.info(
                "Running root cause detection on {} logs",
                logs.size()
        );

        // =====================================================
        // COMBINED LOG TEXT
        // =====================================================
        String combined = logs.stream()
                .map(Log::getMessage)
                .filter(Objects::nonNull)
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        // =====================================================
        // PRIORITY-BASED DETECTION
        // =====================================================

        // =====================================================
        // NULL POINTER
        // =====================================================
        if (containsAny(
                combined,
                "nullpointerexception",
                "null pointer",
                "cannot invoke",
                "cannot read field",
                "because",
                "is null"
        )) {

            return build(
                    "NULL_POINTER_EXCEPTION",
                    95
            );
        }

        // =====================================================
        // OUT OF MEMORY
        // =====================================================
        if (containsAny(
                combined,
                "outofmemoryerror",
                "java heap space",
                "gc overhead limit exceeded",
                "memory leak",
                "heap space"
        )) {

            return build(
                    "MEMORY_ISSUE",
                    94
            );
        }

        // =====================================================
        // DATABASE
        // =====================================================
        if (containsAny(
                combined,
                "sql",
                "jdbc",
                "hibernate",
                "datasource",
                "database",
                "connection pool",
                "constraint violation",
                "deadlock",
                "duplicate key"
        )) {

            return build(
                    "DATABASE_ERROR",
                    90
            );
        }

        // =====================================================
        // TIMEOUT
        // =====================================================
        if (containsAny(
                combined,
                "timeout",
                "timed out",
                "sockettimeout",
                "read timeout",
                "connection timeout",
                "gateway timeout"
        )) {

            return build(
                    "TIMEOUT",
                    88
            );
        }

        // =====================================================
        // AUTHENTICATION
        // =====================================================
        if (containsAny(
                combined,
                "unauthorized",
                "forbidden",
                "access denied",
                "jwt",
                "invalid token",
                "token expired",
                "authentication failed"
        )) {

            return build(
                    "AUTHENTICATION_FAILURE",
                    87
            );
        }

        // =====================================================
        // PAYMENT
        // =====================================================
        if (containsAny(
                combined,
                "payment",
                "gateway",
                "transaction failed",
                "razorpay",
                "stripe",
                "upi",
                "refund failed"
        )) {

            return build(
                    "PAYMENT_FAILURE",
                    90
            );
        }

        // =====================================================
        // NETWORK
        // =====================================================
        if (containsAny(
                combined,
                "connection refused",
                "host unreachable",
                "dns",
                "network",
                "broken pipe",
                "ssl",
                "certificate"
        )) {

            return build(
                    "NETWORK_FAILURE",
                    85
            );
        }

        // =====================================================
        // FILE SYSTEM
        // =====================================================
        if (containsAny(
                combined,
                "file not found",
                "access denied",
                "disk full",
                "ioexception",
                "permission denied"
        )) {

            return build(
                    "FILE_SYSTEM_ERROR",
                    84
            );
        }

        // =====================================================
        // MICROSERVICE FAILURE
        // =====================================================
        if (containsAny(
                combined,
                "service unavailable",
                "503",
                "feign",
                "downstream",
                "circuit breaker",
                "fallback"
        )) {

            return build(
                    "MICROSERVICE_FAILURE",
                    86
            );
        }

        // =====================================================
        // HIGH ERROR VOLUME
        // =====================================================
        long errorCount = logs.stream()
                .filter(log ->
                        log.getLevel() == Log.LogLevel.ERROR
                )
                .count();

        if (errorCount > 20) {

            return build(
                    "HIGH_ERROR_RATE",
                    78
            );
        }

        // =====================================================
        // EXCEPTION EXTRACTION
        // =====================================================
        String detectedException =
                extractTopException(logs);

        if (detectedException != null) {

            return RootCauseResult.builder()
                    .rootCause(detectedException)
                    .confidence(72)
                    .build();
        }

        // =====================================================
        // FALLBACK
        // =====================================================
        return build(
                "APPLICATION_ERROR",
                60
        );
    }

    // =====================================================
    // HELPER - BUILD RESPONSE
    // =====================================================
    private RootCauseResult build(
            String cause,
            int confidence
    ) {

        log.info(
                "Root cause detected={} confidence={}",
                cause,
                confidence
        );

        return RootCauseResult.builder()
                .rootCause(cause)
                .confidence(confidence)
                .build();
    }

    // =====================================================
    // HELPER - KEYWORD MATCH
    // =====================================================
    private boolean containsAny(
            String text,
            String... keywords
    ) {

        for (String keyword : keywords) {

            if (text.contains(
                    keyword.toLowerCase()
            )) {

                return true;
            }
        }

        return false;
    }

    // =====================================================
    // HELPER - EXCEPTION EXTRACTION
    // =====================================================
    private String extractTopException(
            List<Log> logs
    ) {

        return logs.stream()
                .map(Log::getMessage)
                .filter(Objects::nonNull)
                .filter(msg ->
                        msg.contains("Exception")
                                || msg.contains("Error")
                )
                .sorted(
                        Comparator.comparingInt(String::length)
                                .reversed()
                )
                .findFirst()
                .map(this::cleanExceptionName)
                .orElse(null);
    }

    // =====================================================
    // CLEAN EXCEPTION NAME
    // =====================================================
    private String cleanExceptionName(
            String message
    ) {

        String[] parts = message.split("[:\\s]");

        for (String part : parts) {

            if (part.endsWith("Exception")
                    || part.endsWith("Error")) {

                return part.toUpperCase();
            }
        }

        return "APPLICATION_EXCEPTION";
    }

    // =====================================================
    // STEP 10.5 COMPATIBILITY METHOD
    // =====================================================
    public String detect(
            List<Log> logs,
            String aiRootCause
    ) {

        RootCauseResult result =
                detectRootCause(logs);

        if (result.getConfidence() >= 80) {

            return result.getRootCause();
        }

        return aiRootCause != null
                ? aiRootCause
                : result.getRootCause();
    }
}