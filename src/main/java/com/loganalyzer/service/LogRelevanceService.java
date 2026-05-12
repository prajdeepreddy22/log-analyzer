package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LogRelevanceService {

    public List<Log> filterRelevantLogs(String question, List<Log> logs) {

        if (question == null || question.isBlank()) {
            return logs.stream().limit(50).toList();
        }

        Set<String> keywords = extractKeywords(question);

        return logs.stream()
                .map(log -> Map.entry(log, score(log, keywords)))
                .filter(entry -> entry.getValue() > 0)
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .limit(50)
                .collect(Collectors.toList());
    }

    private int score(Log log, Set<String> keywords) {

        String text = (log.getMessage() + " " + log.getLevel().name())
                .toLowerCase();

        int score = 0;

        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                score += 5;
            }
        }

        return score;
    }

    private Set<String> extractKeywords(String question) {

        return Arrays.stream(question.toLowerCase()
                        .replaceAll("[^a-z0-9 ]", " ")
                        .split("\\s+"))
                .filter(word -> word.length() > 3)
                .collect(Collectors.toSet());
    }
}