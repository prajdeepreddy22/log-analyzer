package com.loganalyzer.service;

import org.springframework.stereotype.Service;

@Service
public class ResponseQualityService {

    public boolean isWeakResponse(String answer) {

        if (answer == null || answer.isBlank()) {
            return true;
        }

        String normalized = answer.toLowerCase();

        // =====================================================
        // WEAK RESPONSE DETECTION
        // =====================================================
        if (normalized.contains("insufficient information")) {
            return true;
        }

        if (normalized.contains("not enough information")) {
            return true;
        }

        if (normalized.contains("unable to determine")) {
            return true;
        }

        if (normalized.contains("cannot determine")) {
            return true;
        }

        if (normalized.contains("i don't know")) {
            return true;
        }

        if (normalized.contains("unknown")) {
            return true;
        }

        // =====================================================
        // VERY SMALL ANSWER
        // =====================================================
        if (answer.length() < 120) {
            return true;
        }

        // =====================================================
        // MISSING TECHNICAL CONTENT
        // =====================================================
        boolean noTechnicalTerms =
                !normalized.contains("error")
                        && !normalized.contains("exception")
                        && !normalized.contains("root cause")
                        && !normalized.contains("fix")
                        && !normalized.contains("issue")
                        && !normalized.contains("failure");

        if (noTechnicalTerms) {
            return true;
        }

        return false;
    }
}