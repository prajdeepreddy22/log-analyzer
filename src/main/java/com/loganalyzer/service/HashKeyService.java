package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import com.loganalyzer.parser.ParsedLogEntry;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HashKeyService {

    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("([A-Za-z0-9_.]*Exception|[A-Za-z0-9_.]*Error)");

    // =========================
    // Parser Hash (per log)
    // =========================
    public String computeHash(ParsedLogEntry entry) {

        String message = entry.getMessage() != null ? entry.getMessage() : "";
        String normalized = normalize(message);

        String base;

        if (entry.getLevel() != null &&
                (entry.getLevel() == LogLevel.ERROR ||
                        entry.getLevel() == LogLevel.FATAL)) {

            String exception = extractExceptionClass(message);
            base = exception + "|" + normalized;

        } else {
            base = normalized;
        }

        return sha256(base);
    }

    // =========================
    // AI Hash (multiple logs)
    // =========================
    public String computeHashFromLogs(List<Log> logs) {

        if (logs == null || logs.isEmpty()) {
            return sha256("empty");
        }

        StringBuilder sb = new StringBuilder();

        for (Log log : logs) {
            sb.append(log.getLevel())
                    .append("|")
                    .append(normalize(log.getMessage()))
                    .append("\n");
        }

        return sha256(sb.toString());
    }

    // =========================
    //Normalize
    // =========================
    private String normalize(String input) {
        return input
                .replaceAll("\\d+", "")
                .replaceAll("[0-9a-f-]{36}", "")
                .replaceAll("0x[0-9a-fA-F]+", "")
                .replaceAll("\\d{4}-\\d{2}-\\d{2}.*?\\s", "")
                .toLowerCase()
                .trim();
    }

    // =========================
    //Extract Exception
    // =========================
    private String extractExceptionClass(String message) {

        Matcher matcher = EXCEPTION_PATTERN.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    // =========================
    // SHA-256
    // =========================
    private String sha256(String input) {

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Hash generation failed", e);
        }
    }
}