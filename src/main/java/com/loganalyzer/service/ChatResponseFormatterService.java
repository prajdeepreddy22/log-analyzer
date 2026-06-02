package com.loganalyzer.service;

import org.springframework.stereotype.Service;

@Service
public class ChatResponseFormatterService {

    public String format(String answer) {

        if (answer == null || answer.isBlank()) {
            return answer;
        }

        String formatted = answer
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();

        formatted = formatted.replaceAll("(?<=[.!?])(?=\\d+\\.(?=\\S))", "\n\n");
        formatted = formatted.replaceAll("(?m)(^|\\n)(\\d+)\\.(?=\\S)", "$1$2. ");
        formatted = formatted.replaceAll("(?m)^(\\d+\\.\\s)", "\n$1");
        formatted = formatted.replaceAll("(?<=[.!?])(?=[A-Z])", " ");
        formatted = formatted.replaceAll("(?<!\\n)(Timestamp:)", "\n$1");
        formatted = formatted.replaceAll("(?<!\\n)(Evidence:)", "\n\n$1");
        formatted = formatted.replaceAll("(?<!\\n)(Fix:)", "\n\n$1");
        formatted = formatted.replaceAll("(?<!\\n)(Fix Suggestions:)", "\n\n$1");
        formatted = formatted.replaceAll("(?m)^(Evidence:|Fix:)", "\n$1");
        formatted = formatted.replaceAll("\\.\\s*-\\s*", ".\n- ");
        formatted = formatted.replaceAll("(?m)^\\s*-\\s+", "- ");
        formatted = formatted.replaceAll("(?m)[ \\t]+$", "");
        formatted = formatted.replaceAll("\\n{3,}", "\n\n");

        return formatted.trim();
    }
}
