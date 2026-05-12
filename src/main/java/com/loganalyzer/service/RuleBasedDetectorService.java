package com.loganalyzer.service;

import com.loganalyzer.dto.response.RuleMatchResult;
import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedDetectorService {

    public List<RuleMatchResult> detect(List<Log> logs) {

        List<RuleMatchResult> results = new ArrayList<>();

        String combined = logs.stream()
                .map(Log::getMessage)
                .reduce("", (a, b) -> a + " " + b)
                .toLowerCase();

        // =====================================================
        // NULL POINTER
        // =====================================================
        if (combined.contains("nullpointerexception")) {

            results.add(
                    RuleMatchResult.builder()
                            .category("JAVA_EXCEPTION")
                            .description("NullPointerException detected")
                            .severity(9)
                            .matched(true)
                            .build()
            );
        }

        // =====================================================
        // DATABASE
        // =====================================================
        if (combined.contains("connection refused")
                || combined.contains("jdbc")
                || combined.contains("datasource")) {

            results.add(
                    RuleMatchResult.builder()
                            .category("DATABASE")
                            .description("Database connectivity issue detected")
                            .severity(10)
                            .matched(true)
                            .build()
            );
        }

        // =====================================================
        // TIMEOUT
        // =====================================================
        if (combined.contains("timeout")) {

            results.add(
                    RuleMatchResult.builder()
                            .category("TIMEOUT")
                            .description("Timeout issue detected")
                            .severity(8)
                            .matched(true)
                            .build()
            );
        }

        // =====================================================
        // MEMORY
        // =====================================================
        if (combined.contains("outofmemoryerror")) {

            results.add(
                    RuleMatchResult.builder()
                            .category("MEMORY")
                            .description("Memory issue detected")
                            .severity(10)
                            .matched(true)
                            .build()
            );
        }

        // =====================================================
        // AUTH
        // =====================================================
        if (combined.contains("unauthorized")
                || combined.contains("forbidden")
                || combined.contains("access denied")) {

            results.add(
                    RuleMatchResult.builder()
                            .category("AUTH")
                            .description("Authentication/Authorization issue detected")
                            .severity(7)
                            .matched(true)
                            .build()
            );
        }

        return results;
    }
}