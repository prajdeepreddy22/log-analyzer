package com.loganalyzer.event;

public record AnalysisCompletedEvent(
        Long userId,
        Long analysisId,
        String uploadId,
        String status,
        String confidence
) {
}
