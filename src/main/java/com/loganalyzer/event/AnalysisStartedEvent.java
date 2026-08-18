package com.loganalyzer.event;

public record AnalysisStartedEvent(
        Long userId,
        Long analysisId,
        String uploadId
) {
}
