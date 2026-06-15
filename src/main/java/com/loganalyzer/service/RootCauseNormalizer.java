package com.loganalyzer.service;

import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.Locale;

@Component
@Slf4j
public class RootCauseNormalizer {

    public enum RootCauseCategory {
        NETWORK_TIMEOUT,
        DATABASE_CONNECTIVITY_FAILURE,
        MEMORY_EXHAUSTION,
        NULL_REFERENCE_ERROR,
        INTERNAL_SERVER_FAILURE,
        UNKNOWN_ERROR
    }

    public String normalize(String rawRootCause) {

        if (rawRootCause == null || rawRootCause.isBlank()) {
            return RootCauseCategory.UNKNOWN_ERROR.name();
        }

        String normalized = rawRootCause
                .trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_");

        try {
            return RootCauseCategory.valueOf(normalized).name();
        } catch (IllegalArgumentException ignored) {
            log.warn(
                    "Normalizing non-standard root cause value={}",
                    abbreviate(rawRootCause)
            );
        }

        if (matchesAny(
                normalized,
                "NETWORK_TIMEOUT",
                "TIMEOUT",
                "TIMED_OUT",
                "SOCKET_TIMEOUT",
                "SOCKETTIMEOUT",
                "READ_TIMEOUT",
                "CONNECTION_TIMEOUT",
                "GATEWAY_TIMEOUT"
        )) {
            return RootCauseCategory.NETWORK_TIMEOUT.name();
        }

        if (matchesAny(
                normalized,
                "DATABASE_CONNECTIVITY_FAILURE",
                "DATABASE_ERROR",
                "DATABASE_FAILURE",
                "SQL_EXCEPTION",
                "SQLEXCEPTION",
                "JDBC",
                "HIBERNATE",
                "DATASOURCE",
                "CONNECTION_POOL",
                "DEADLOCK",
                "CONSTRAINT_VIOLATION",
                "DUPLICATE_KEY"
        )) {
            return RootCauseCategory.DATABASE_CONNECTIVITY_FAILURE.name();
        }

        if (matchesAny(
                normalized,
                "MEMORY_EXHAUSTION",
                "MEMORY_ISSUE",
                "OUT_OF_MEMORY",
                "OUTOFMEMORY",
                "OUTOFMEMORYERROR",
                "JAVA_HEAP_SPACE",
                "HEAP_SPACE",
                "GC_OVERHEAD",
                "MEMORY_LEAK"
        )) {
            return RootCauseCategory.MEMORY_EXHAUSTION.name();
        }

        if (matchesAny(
                normalized,
                "NULL_REFERENCE_ERROR",
                "NULL_POINTER_EXCEPTION",
                "NULLPOINTEREXCEPTION",
                "NULL_POINTER",
                "CANNOT_INVOKE",
                "IS_NULL"
        )) {
            return RootCauseCategory.NULL_REFERENCE_ERROR.name();
        }

        if (matchesAny(
                normalized,
                "INTERNAL_SERVER_FAILURE",
                "INTERNAL_SERVER_ERROR",
                "APPLICATION_ERROR",
                "APPLICATION_EXCEPTION",
                "AUTHENTICATION_FAILURE",
                "PAYMENT_FAILURE",
                "NETWORK_FAILURE",
                "FILE_SYSTEM_ERROR",
                "MICROSERVICE_FAILURE",
                "HIGH_ERROR_RATE",
                "SERVICE_UNAVAILABLE",
                "EXCEPTION",
                "ERROR"
        )) {
            return RootCauseCategory.INTERNAL_SERVER_FAILURE.name();
        }

        return RootCauseCategory.UNKNOWN_ERROR.name();
    }

    public boolean isAllowed(String rootCause) {

        if (rootCause == null || rootCause.isBlank()) {
            return false;
        }

        try {
            RootCauseCategory.valueOf(rootCause);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private boolean matchesAny(String value, String... aliases) {

        for (String alias : aliases) {
            if (value.equals(alias)
                    || value.startsWith(alias + "_")
                    || value.endsWith("_" + alias)
                    || value.contains("_" + alias + "_")) {
                return true;
            }
        }

        return false;
    }

    private String abbreviate(String value) {
        return value.length() <= 200
                ? value
                : value.substring(0, 200);
    }
}
