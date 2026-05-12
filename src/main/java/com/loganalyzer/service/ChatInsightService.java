package com.loganalyzer.service;

import com.loganalyzer.dto.response.RootCauseResult;
import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatInsightService {

    public List<String> generateInsights(
            List<Log> logs,
            RootCauseResult rootCauseResult,
            int confidence
    ) {

        List<String> insights = new ArrayList<>();

        if (rootCauseResult != null) {

            insights.add(
                    "Detected root cause: "
                            + rootCauseResult.getRootCause()
            );

            insights.add(
                    "Root cause confidence: "
                            + rootCauseResult.getConfidence()
                            + "%"
            );
        }

        long errorCount = logs.stream()
                .filter(log -> log.getLevel() == Log.LogLevel.ERROR)
                .count();

        if (errorCount > 0) {

            insights.add(
                    "Detected " + errorCount + " ERROR logs"
            );
        }

        if (confidence >= 80) {

            insights.add(
                    "AI analysis confidence is HIGH"
            );

        } else if (confidence >= 50) {

            insights.add(
                    "AI analysis confidence is MEDIUM"
            );

        } else {

            insights.add(
                    "AI analysis confidence is LOW"
            );
        }

        return insights;
    }
}