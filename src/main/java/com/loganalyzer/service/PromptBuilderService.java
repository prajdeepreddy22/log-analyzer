package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilderService {

    @Value("${app.ai.max-logs:100}")
    private int maxAnalysisLogs;

    @Value("${app.ai.max-chars:12000}")
    private int maxAnalysisChars;

    // =====================================================
    // ANALYSIS PROMPT
    // =====================================================
    public String buildAnalysisPrompt(List<Log> logs) {

        StringBuilder sb = new StringBuilder();

        sb.append("You are a senior backend engineer.\n\n");

        sb.append("Analyze logs and return JSON:\n");

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

        sb.append("\n\nLOGS:\n");

        logs.stream()
                .limit(maxAnalysisLogs)
                .map(this::formatLog)
                .takeWhile(line -> sb.length() + line.length() <= maxAnalysisChars)
                .forEach(sb::append);

        return sb.toString();
    }

    // =====================================================
    // CHAT PROMPT
    // =====================================================
    public String buildChatPrompt(
            String question,
            List<Log> logs
    ) {

        StringBuilder sb = new StringBuilder();

        sb.append("""
                You are a senior backend engineer.

                Answer ONLY using provided logs.

                If answer is not found in logs,
                clearly say insufficient evidence.

                Format the answer cleanly:
                - Use this exact structure:
                  Issues

                  1. <issue title>

                  Evidence:
                  - <log evidence>

                  Fix:
                  - <specific fix>
                - Put a blank line between different issues.
                - Group related WARN/INFO retry or recovery events under the nearest related ERROR.
                - Do not list retry, recovery, or follow-up events as separate issues unless they are independent failures.
                - Prefer the smallest set of primary issues.
                - Always include a space after punctuation.
                - Keep the answer concise and technical.

                USER QUESTION:
                """);

        sb.append(question).append("\n\n");

        sb.append("RELEVANT LOGS:\n\n");

        logs.stream()
                .limit(80)
                .forEach(log ->
                        sb.append("[")
                                .append(log.getLevel())
                                .append("] ")
                                .append(log.getServiceName())
                                .append(" : ")
                                .append(truncate(log.getMessage()))
                                .append("\n")
                );

        return sb.toString();
    }

    // =====================================================
    // TRUNCATE
    // =====================================================
    private String truncate(String msg) {

        return msg != null && msg.length() > 300
                ? msg.substring(0, 300)
                : msg;
    }

    private String formatLog(Log log) {

        return "["
                + log.getLevel()
                + "] "
                + log.getServiceName()
                + " : "
                + truncate(log.getMessage())
                + "\n";
    }
}
