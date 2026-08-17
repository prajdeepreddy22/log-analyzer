package com.loganalyzer.event;

public record LogIngestedEvent(
        Long userId,
        Long sourceId,
        int count
) {
}
