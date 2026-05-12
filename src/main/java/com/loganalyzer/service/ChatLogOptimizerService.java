package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatLogOptimizerService {

    public List<Log> optimize(List<Log> logs) {

        if (logs == null || logs.isEmpty()) {
            return List.of();
        }

        // 1. Remove duplicates (based on message + level)
        Map<String, Log> uniqueMap = new LinkedHashMap<>();

        for (Log log : logs) {

            String key = generateKey(log);

            uniqueMap.putIfAbsent(key, log);
        }

        List<Log> uniqueLogs = new ArrayList<>(uniqueMap.values());

        // 2. Trim heavy messages
        for (Log log : uniqueLogs) {
            log.setMessage(trimMessage(log.getMessage()));
        }

        // 3. Limit final size (token safety)
        return uniqueLogs.stream()
                .limit(80)
                .collect(Collectors.toList());
    }

    // =====================================================
    // CREATE UNIQUE KEY
    // =====================================================
    private String generateKey(Log log) {

        return log.getLevel() +
                "|" +
                normalize(log.getMessage());
    }

    // =====================================================
    // NORMALIZE MESSAGE
    // =====================================================
    private String normalize(String message) {

        if (message == null) return "";

        return message
                .toLowerCase()
                .replaceAll("\\d+", "")          // remove numbers
                .replaceAll("[a-f0-9\\-]{10,}", "") // remove ids/uuid-like strings
                .trim();
    }

    // =====================================================
    // TRIM MESSAGE
    // =====================================================
    private String trimMessage(String message) {

        if (message == null) return "";

        if (message.length() > 200) {
            return message.substring(0, 200) + "...";
        }

        return message;
    }
}