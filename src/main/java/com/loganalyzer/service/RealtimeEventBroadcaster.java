package com.loganalyzer.service;

import com.loganalyzer.event.AnalysisCompletedEvent;
import com.loganalyzer.event.AnalysisStartedEvent;
import com.loganalyzer.event.IncidentStatusChangedEvent;
import com.loganalyzer.event.LogIngestedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RealtimeEventBroadcaster {

    private final SseEmitterRegistry registry;

    @EventListener
    public void onLogIngested(LogIngestedEvent event) {
        registry.sendToUser(
                event.userId(),
                "LOG_INGESTED",
                Map.of(
                        "sourceId", event.sourceId(),
                        "count", event.count()
                )
        );
    }

    @EventListener
    public void onAnalysisStarted(AnalysisStartedEvent event) {
        registry.sendToUser(
                event.userId(),
                "ANALYSIS_STARTED",
                Map.of(
                        "analysisId", event.analysisId(),
                        "uploadId", event.uploadId()
                )
        );
    }

    @EventListener
    public void onAnalysisCompleted(AnalysisCompletedEvent event) {
        registry.sendToUser(
                event.userId(),
                "ANALYSIS_COMPLETED",
                Map.of(
                        "analysisId", event.analysisId(),
                        "uploadId", event.uploadId(),
                        "status", event.status(),
                        "confidence", event.confidence()
                )
        );
    }

    @EventListener
    public void onIncidentStatusChanged(IncidentStatusChangedEvent event) {
        registry.sendToUser(
                event.userId(),
                "INCIDENT_STATUS_CHANGED",
                Map.of(
                        "incidentId", event.incidentId(),
                        "fromStatus", event.fromStatus(),
                        "toStatus", event.toStatus()
                )
        );
    }
}
