package com.loganalyzer.service;

import com.loganalyzer.event.AnalysisCompletedEvent;
import com.loganalyzer.event.AnalysisStartedEvent;
import com.loganalyzer.event.IncidentStatusChangedEvent;
import com.loganalyzer.event.LogIngestedEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeEventBroadcasterTest {

    private final SseEmitterRegistry registry =
            mock(SseEmitterRegistry.class);

    private final RealtimeEventBroadcaster broadcaster =
            new RealtimeEventBroadcaster(registry);

    @Test
    void broadcastsLogIngestedToEventOwner() {

        broadcaster.onLogIngested(new LogIngestedEvent(1L, 12L, 4));

        verify(registry)
                .sendToUser(eq(1L), eq("LOG_INGESTED"), any());
    }

    @Test
    void broadcastsAnalysisStartedToEventOwner() {

        broadcaster.onAnalysisStarted(
                new AnalysisStartedEvent(1L, 10L, "upload-1")
        );

        verify(registry)
                .sendToUser(eq(1L), eq("ANALYSIS_STARTED"), any());
    }

    @Test
    void broadcastsAnalysisCompletedToEventOwner() {

        broadcaster.onAnalysisCompleted(
                new AnalysisCompletedEvent(
                        1L,
                        10L,
                        "upload-1",
                        "COMPLETED",
                        "0.900"
                )
        );

        verify(registry)
                .sendToUser(eq(1L), eq("ANALYSIS_COMPLETED"), any());
    }

    @Test
    void broadcastsIncidentStatusChangedToEventOwner() {

        broadcaster.onIncidentStatusChanged(
                new IncidentStatusChangedEvent(
                        1L,
                        "incident-1",
                        "OPEN",
                        "INVESTIGATING"
                )
        );

        verify(registry)
                .sendToUser(eq(1L), eq("INCIDENT_STATUS_CHANGED"), any());
    }
}
