package com.loganalyzer.service;

import com.loganalyzer.dto.response.AnomalyResult;
import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {

    public List<AnomalyResult> detectAnomalies(
            List<Log> logs
    ) {

        List<AnomalyResult> anomalies =
                new ArrayList<>();

        long errorCount = logs.stream()
                .filter(log ->
                        log.getLevel() == Log.LogLevel.ERROR
                )
                .count();

        // =====================================================
        // ERROR SPIKE
        // =====================================================
        if (errorCount >= 15) {

            anomalies.add(
                    AnomalyResult.builder()
                            .type("ERROR_SPIKE")
                            .message(
                                    "Unusual spike in ERROR logs detected"
                            )
                            .severity(9)
                            .build()
            );
        }

        // =====================================================
        // REPEATED FAILURES
        // =====================================================
        long repeatedFailures = logs.stream()
                .filter(log ->
                        log.getMessage() != null
                                && log.getMessage()
                                .toLowerCase()
                                .contains("failed")
                )
                .count();

        if (repeatedFailures >= 10) {

            anomalies.add(
                    AnomalyResult.builder()
                            .type("REPEATED_FAILURES")
                            .message(
                                    "Repeated failure patterns detected"
                            )
                            .severity(8)
                            .build()
            );
        }

        // =====================================================
        // TIMEOUT ANOMALY
        // =====================================================
        long timeoutCount = logs.stream()
                .filter(log ->
                        log.getMessage() != null
                                && log.getMessage()
                                .toLowerCase()
                                .contains("timeout")
                )
                .count();

        if (timeoutCount >= 5) {

            anomalies.add(
                    AnomalyResult.builder()
                            .type("TIMEOUT_ANOMALY")
                            .message(
                                    "Large number of timeout events detected"
                            )
                            .severity(7)
                            .build()
            );
        }

        return anomalies;
    }
}