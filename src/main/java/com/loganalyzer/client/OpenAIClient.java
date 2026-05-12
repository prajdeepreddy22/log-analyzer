package com.loganalyzer.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
public class OpenAIClient {

    // =========================================================
    // ANALYSIS AI
    // =========================================================
    public Map<String, Object> analyzeLogs(String prompt) {

        log.info(
                "Sending analysis request to OpenAI | promptLength={}",
                safeLength(prompt)
        );

        return Map.of(
                "summary", "Payment failure due to timeout",
                "root_cause", "External payment gateway timeout",
                "developer_mistake", "Retry handling was not implemented correctly",
                "fix_suggestion", "Add retry handling with exponential backoff and circuit breaker",
                "code_fix",
                """
                RetryTemplate retryTemplate = new RetryTemplate();

                retryTemplate.execute(context ->
                        paymentGateway.process(request)
                );
                """,
                "severity_score", 4
        );
    }

    // =========================================================
    // NORMAL CHAT AI
    // =========================================================
    public String askQuestion(String prompt) {

        log.info(
                "Sending chat request to OpenAI | promptLength={}",
                safeLength(prompt)
        );

        return buildMockResponse();
    }

    // =========================================================
    // STREAMING AI (MOCK FOR NOW)
    // =========================================================
    public String streamQuestion(String prompt) {

        log.info(
                "Streaming AI response | promptLength={}",
                safeLength(prompt)
        );

        return buildMockResponse();
    }

    // =========================================================
    // CENTRAL MOCK RESPONSE
    // =========================================================
    private String buildMockResponse() {

        return """
                The application failure is primarily caused by repeated
                payment gateway timeout exceptions.

                Root Cause:
                External dependency instability and missing retry logic.

                Impact:
                - Transaction failures
                - Increased latency under load

                Fix:
                - Add retry with exponential backoff
                - Add circuit breaker
                - Improve timeout configuration
                """;
    }

    // =========================================================
    // SAFETY HELPER
    // =========================================================
    private int safeLength(String prompt) {

        return prompt == null ? 0 : prompt.length();
    }
}