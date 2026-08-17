package com.loganalyzer.event;

public record IncidentStatusChangedEvent(
        Long userId,
        String incidentId,
        String fromStatus,
        String toStatus
) {
}
