package com.loganalyzer.parser;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HashKeyService {

    // Detect exception names (NullPointerException, IOException, etc.)
    private static final Pattern EXCEPTION_PATTERN =
            Pattern.compile("([A-Za-z0-9_.]*Exception|[A-Za-z0-9_.]*Error)");

    // ==================== MAIN METHOD ====================

    public String computeHash(ParsedLogEntry entry) {

        String message = entry.getMessage() != null ? entry.getMessage() : "";

        // Normalize message
        String normalized = normalize(message);

        String base;

        // Different strategy for ERROR/FATAL
        if (entry.getLevel() == LogLevel.ERROR ||
                entry.getLevel() == LogLevel.FATAL) {

            String exception = extractExceptionClass(message);

            base = exception + "|" + normalized;

        } else {
            base = normalized;
        }

        return sha256(base);
    }

    // ==================== NORMALIZATION ====================

    private String normalize(String input) {

        return input
                .replaceAll("\\d+", "")                     // remove numbers
                .replaceAll("[0-9a-f-]{36}", "")           // remove UUID
                .replaceAll("0x[0-9a-fA-F]+", "")          // remove hex
                .replaceAll("\\d{4}-\\d{2}-\\d{2}.*?\\s", "") // remove timestamps
                .toLowerCase()
                .trim();
    }

    // ==================== EXCEPTION EXTRACTION ====================

    private String extractExceptionClass(String message) {

        Matcher matcher = EXCEPTION_PATTERN.matcher(message);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    // ==================== SHA-256 ====================

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
            throw new RuntimeException("Failed to generate hash", e);
        }
    }
}