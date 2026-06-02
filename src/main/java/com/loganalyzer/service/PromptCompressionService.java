package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PromptCompressionService {

    @Value("${app.ai.max-compressed-logs:25}")
    private int maxCompressedLogs;

    @Value("${app.ai.max-message-length:180}")
    private int maxMessageLength;

    public List<Log> compressLogs(List<Log> logs) {

        if (logs == null || logs.isEmpty()) {
            return Collections.emptyList();
        }

        log.info(
                "Compressing logs. originalCount={}",
                logs.size()
        );

        // =====================================================
        // REMOVE DUPLICATES
        // =====================================================
        Map<String, Log> uniqueLogs =
                new LinkedHashMap<>();

        for (Log log : logs) {

            if (log.getMessage() == null) {
                continue;
            }

            String normalized =
                    normalize(log.getMessage());

            uniqueLogs.putIfAbsent(
                    normalized,
                    cloneAndTrim(log)
            );
        }

        // =====================================================
        // PRIORITIZE IMPORTANT LOGS
        // =====================================================
        List<Log> compressed = uniqueLogs.values()
                .stream()
                .sorted(this::comparePriority)
                .limit(maxCompressedLogs)
                .map(this::truncateMessage)
                .collect(Collectors.toList());

        log.info(
                "Compression completed. compressedCount={}",
                compressed.size()
        );

        return compressed;
    }

    // =====================================================
    // PROMPT STRING COMPRESSION
    // =====================================================
    public String compress(String prompt) {

        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        log.info(
                "Compressing prompt. originalLength={}",
                prompt.length()
        );

        String compressed = prompt

                // remove extra spaces
                .replaceAll("[ \\t]+", " ")

                // remove excessive line breaks
                .replaceAll("\\n{3,}", "\n\n")

                // replace verbose instructions
                .replace(
                        "Provide a more detailed technical analysis with root cause reasoning and fix suggestions.",
                        "Provide detailed RCA and fixes."
                )
                .replace(
                        "You are a senior backend engineer and production incident investigator.",
                        "You are an expert production incident analyzer."
                )
                .replace(
                        "Keep the response concise but technically strong.",
                        "Be concise and technical."
                )
                .trim();

        log.info(
                "Prompt compression completed. compressedLength={}",
                compressed.length()
        );

        return compressed;
    }

    // =====================================================
    // PRIORITY SORTING
    // =====================================================
    private int comparePriority(Log a, Log b) {

        int aPriority = getPriority(a);

        int bPriority = getPriority(b);

        return Integer.compare(aPriority, bPriority);
    }

    private int getPriority(Log log) {

        if (log.getLevel() == null) {
            return 99;
        }

        return switch (log.getLevel()) {

            case ERROR -> 1;

            case WARN -> 2;

            case INFO -> 3;

            case DEBUG -> 4;

            default -> 5;
        };
    }

    // =====================================================
    // SAFE CLONE
    // =====================================================
    private Log cloneAndTrim(Log source) {

        return Log.builder()
                .id(source.getId())
                .message(source.getMessage())
                .level(source.getLevel())
                .serviceName(source.getServiceName())
                .logTimestamp(source.getLogTimestamp())
                .upload(source.getUpload())
                .build();
    }

    // =====================================================
    // TRUNCATE MESSAGE
    // =====================================================
    private Log truncateMessage(Log log) {

        if (log.getMessage() == null) {
            return log;
        }

        if (log.getMessage().length()
                <= maxMessageLength) {

            return log;
        }

        log.setMessage(
                log.getMessage()
                        .substring(0, maxMessageLength)
                        + "..."
        );

        return log;
    }

    // =====================================================
    // NORMALIZE
    // =====================================================
    private String normalize(String text) {

        return text.toLowerCase()
                .replaceAll("\\d+", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
