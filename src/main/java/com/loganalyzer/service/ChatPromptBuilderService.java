package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ChatPromptBuilderService {

    private static final int MAX_LOGS = 30;

    private static final int MAX_MESSAGE_LENGTH = 250;

    public String buildPrompt(
            String question,
            List<Log> logs,
            Analysis analysis,
            List<String> ruleFindings,
            List<String> anomalies
    ) {

        StringBuilder prompt = new StringBuilder();

        // =====================================================
        // SYSTEM ROLE
        // =====================================================
        prompt.append("""
                You are a senior backend engineer and production incident investigator.

                Analyze logs carefully and answer accurately.

                Rules:
                - Be technically accurate
                - Focus on root cause
                - Use logs as evidence
                - Mention exceptions
                - Suggest fixes
                - Do not hallucinate
                - Keep response concise
                """);

        // =====================================================
        // EXISTING ANALYSIS
        // =====================================================
        if (analysis != null) {

            prompt.append("\n\nPREVIOUS ANALYSIS:\n");

            append(prompt, "Summary", analysis.getSummary());

            append(prompt,
                    "Root Cause",
                    analysis.getRootCause());

            append(prompt,
                    "Fix",
                    analysis.getFixSuggestion());
        }

        // =====================================================
        // RULE FINDINGS
        // =====================================================
        if (ruleFindings != null
                && !ruleFindings.isEmpty()) {

            prompt.append("\n\nRULE FINDINGS:\n");

            ruleFindings.forEach(rule ->
                    prompt.append("- ")
                            .append(rule)
                            .append("\n"));
        }

        // =====================================================
        // ANOMALIES
        // =====================================================
        if (anomalies != null
                && !anomalies.isEmpty()) {

            prompt.append("\n\nANOMALIES:\n");

            anomalies.forEach(anomaly ->
                    prompt.append("- ")
                            .append(anomaly)
                            .append("\n"));
        }

        // =====================================================
        // LOGS
        // =====================================================
        prompt.append("\n\nLOGS:\n");

        logs.stream()
                .limit(MAX_LOGS)
                .forEach(log -> {

                    prompt.append("[")
                            .append(log.getLevel())
                            .append("] ");

                    if (log.getServiceName() != null) {

                        prompt.append(log.getServiceName())
                                .append(" -> ");
                    }

                    prompt.append(
                                    truncate(log.getMessage())
                            )
                            .append("\n");
                });

        // =====================================================
        // QUESTION
        // =====================================================
        prompt.append("\n\nQUESTION:\n");
        prompt.append(question);

        // =====================================================
        // OUTPUT FORMAT
        // =====================================================
        prompt.append("""

                Provide:
                1. Root cause
                2. Impact
                3. Evidence
                4. Fix
                5. Prevention
                """);

        return prompt.toString();
    }

    private void append(
            StringBuilder sb,
            String key,
            String value
    ) {

        if (value != null && !value.isBlank()) {

            sb.append(key)
                    .append(": ")
                    .append(value)
                    .append("\n");
        }
    }

    private String truncate(String msg) {

        if (msg == null) {
            return "";
        }

        return msg.length() > MAX_MESSAGE_LENGTH
                ? msg.substring(0, MAX_MESSAGE_LENGTH)
                : msg;
    }
}