package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RulesEngineService {

    public List<String> evaluate(List<Log> logs) {

        List<String> findings = new ArrayList<>();

        long errorCount = logs.stream()
                .filter(log -> log.getLevel() == Log.LogLevel.ERROR)
                .count();

        long warnCount = logs.stream()
                .filter(log -> log.getLevel() == Log.LogLevel.WARN)
                .count();

        // =====================================================
        // HIGH ERROR RATE
        // =====================================================
        if (errorCount >= 10) {

            findings.add(
                    "High ERROR frequency detected in logs"
            );
        }

        // =====================================================
        // HIGH WARNING RATE
        // =====================================================
        if (warnCount >= 20) {

            findings.add(
                    "Large number of WARN logs detected"
            );
        }

        // =====================================================
        // DATABASE ISSUES
        // =====================================================
        boolean dbIssue = logs.stream()
                .anyMatch(log ->
                        contains(log.getMessage(),
                                "database",
                                "sql",
                                "jdbc",
                                "connection refused",
                                "hikari")
                );

        if (dbIssue) {

            findings.add(
                    "Possible database connectivity issue detected"
            );
        }

        // =====================================================
        // MEMORY ISSUES
        // =====================================================
        boolean memoryIssue = logs.stream()
                .anyMatch(log ->
                        contains(log.getMessage(),
                                "outofmemory",
                                "heap",
                                "gc overhead")
                );

        if (memoryIssue) {

            findings.add(
                    "Possible JVM memory pressure detected"
            );
        }

        // =====================================================
        // TIMEOUT ISSUES
        // =====================================================
        boolean timeoutIssue = logs.stream()
                .anyMatch(log ->
                        contains(log.getMessage(),
                                "timeout",
                                "timed out",
                                "socket timeout")
                );

        if (timeoutIssue) {

            findings.add(
                    "Timeout related failures detected"
            );
        }

        return findings;
    }

    private boolean contains(
            String text,
            String... keywords
    ) {

        if (text == null) {
            return false;
        }

        String normalized = text.toLowerCase();

        for (String keyword : keywords) {

            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}