package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromptBuilderService {

    public String buildPrompt(List<Log> logs) {

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
                .limit(50)
                .forEach(log ->
                        sb.append("[")
                                .append(log.getLevel())
                                .append("] ")
                                .append(log.getServiceName())
                                .append(" -> ")
                                .append(truncate(log.getMessage()))
                                .append("\n")
                );

        return sb.toString();
    }

    private String truncate(String msg) {
        return msg != null && msg.length() > 300
                ? msg.substring(0, 300)
                : msg;
    }
}