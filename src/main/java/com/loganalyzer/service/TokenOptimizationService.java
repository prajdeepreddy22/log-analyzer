package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TokenOptimizationService {

    private static final int MAX_LOGS = 80;

    public List<Log> optimizeLogs(List<Log> logs) {

        if (logs == null || logs.isEmpty()) {
            return List.of();
        }

        return logs.stream()
                .filter(log ->
                        log.getLevel() == Log.LogLevel.ERROR
                                || log.getLevel() == Log.LogLevel.WARN
                )
                .limit(MAX_LOGS)
                .toList();
    }
}