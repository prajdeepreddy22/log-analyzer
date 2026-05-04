package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromptBuilderService {

    public String buildPrompt(List<Log> logs) {

        StringBuilder sb = new StringBuilder();

        sb.append("You are a senior backend engineer.\n\n");

        sb.append("Analyze the logs and provide:\n");
        sb.append("1. Summary\n");
        sb.append("2. Root Cause\n");
        sb.append("3. Developer Mistake\n");
        sb.append("4. Fix Suggestion\n");
        sb.append("5. Code Fix\n");
        sb.append("6. Severity Score (1-5)\n\n");

        sb.append("Logs (grouped):\n");

        // GROUP SIMILAR LOGS
        Map<String, Long> groupedLogs = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getLevel() + " | " + log.getServiceName() + " | " + log.getMessage(),
                        Collectors.counting()
                ));

        // LIMIT GROUPS (not raw logs)
        groupedLogs.entrySet().stream()
                .limit(20)
                .forEach(entry ->
                        sb.append(entry.getKey())
                                .append(" (")
                                .append(entry.getValue())
                                .append(" times)\n")
                );

        sb.append("\nRespond strictly in JSON format like:\n");
        sb.append("""
        {
          "summary": "...",
          "root_cause": "...",
          "developer_mistake": "...",
          "fix_suggestion": "...",
          "code_fix": "...",
          "severity_score": 1
        }
        """);

        return sb.toString();
    }
}