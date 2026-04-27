package com.loganalyzer.client;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OpenAIClient {

    public Map<String, Object> analyzeLogs(String prompt) {

        // 🔥 MOCK RESPONSE (for testing full flow)
        return Map.of(
                "summary", "Payment failure due to timeout",
                "root_cause", "External payment gateway timeout",
                "developer_mistake", "No retry handling implemented properly",
                "fix_suggestion", "Add retry with exponential backoff",
                "code_fix", "Implement retry logic in PaymentService",
                "severity_score", 4
        );
    }
}