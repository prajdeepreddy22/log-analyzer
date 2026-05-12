package com.loganalyzer.service;

import com.loganalyzer.dto.response.RootCauseResult;
import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AIConfidenceService {

    public int calculateConfidence(
            List<Log> logs,
            RootCauseResult rootCauseResult,
            String answer
    ) {

        int score = 0;

        // =====================================================
        // LOG VOLUME SCORE
        // =====================================================
        if (logs != null) {

            if (logs.size() >= 50) {
                score += 30;
            }
            else if (logs.size() >= 20) {
                score += 20;
            }
            else if (logs.size() >= 10) {
                score += 10;
            }
            else {
                score += 5;
            }
        }

        // =====================================================
        // ROOT CAUSE CONFIDENCE
        // =====================================================
        if (rootCauseResult != null) {

            score += Math.min(
                    rootCauseResult.getConfidence(),
                    40
            );

            if (!"UNKNOWN".equalsIgnoreCase(
                    rootCauseResult.getRootCause()
            )) {

                score += 5;
            }
        }

        // =====================================================
        // ANSWER QUALITY SCORE
        // =====================================================
        if (answer != null && !answer.isBlank()) {

            int length = answer.length();

            if (length > 500) {
                score += 30;
            }
            else if (length > 250) {
                score += 20;
            }
            else if (length > 100) {
                score += 10;
            }

            String normalized = answer.toLowerCase();

            if (normalized.contains("root cause")) {
                score += 5;
            }

            if (normalized.contains("fix")) {
                score += 5;
            }

            if (normalized.contains("exception")
                    || normalized.contains("timeout")
                    || normalized.contains("failed")
                    || normalized.contains("error")) {

                score += 5;
            }

            if (normalized.contains("anomaly")) {
                score += 5;
            }

            if (normalized.contains("memory")
                    || normalized.contains("cpu")
                    || normalized.contains("latency")) {

                score += 5;
            }
        }

        // =====================================================
        // NORMALIZE
        // =====================================================
        return Math.min(score, 100);
    }

    public String determineQuality(int confidence) {

        if (confidence >= 80) {
            return "HIGH";
        }

        if (confidence >= 50) {
            return "MEDIUM";
        }

        return "LOW";
    }
}