package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Incident;
import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.entity.IncidentStatusHistory;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.User;
import com.loganalyzer.event.IncidentStatusChangedEvent;
import com.loganalyzer.repository.AnalysisRepository;
import com.loganalyzer.repository.IncidentRepository;
import com.loganalyzer.repository.IncidentStatusHistoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.atLeastOnce;

class IncidentGroupingServiceTest {

    private final IncidentRepository incidentRepository =
            mock(IncidentRepository.class);

    private final AnalysisRepository analysisRepository =
            mock(AnalysisRepository.class);

    private final IncidentStatusHistoryRepository historyRepository =
            mock(IncidentStatusHistoryRepository.class);

    private final ApplicationEventPublisher eventPublisher =
            mock(ApplicationEventPublisher.class);

    private final IncidentGroupingService service =
            new IncidentGroupingService(
                    incidentRepository,
                    analysisRepository,
                    historyRepository,
                    eventPublisher
            );

    @Test
    void createsIncidentForCompletedAnalysis() {

        Analysis analysis = completedAnalysis(
                1L,
                4,
                0.90,
                LocalDateTime.of(2026, 6, 10, 10, 0)
        );

        when(incidentRepository
                .findByUploadUploadIdAndUserIdAndRootCause(
                        "upload-1",
                        1L,
                        "NULL_REFERENCE_ERROR"
                ))
                .thenReturn(Optional.empty());
        when(analysisRepository.findByIncidentIncidentId(any()))
                .thenReturn(List.of());
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.group(analysis);

        ArgumentCaptor<Incident> incidentCaptor =
                ArgumentCaptor.forClass(Incident.class);

        verify(incidentRepository, atLeastOnce())
                .save(incidentCaptor.capture());

        Incident incident = analysis.getIncident();

        assertThat(analysis.getIncident()).isSameAs(incident);
        assertThat(incident.getRootCause())
                .isEqualTo("NULL_REFERENCE_ERROR");
        assertThat(incident.getTitle())
                .isEqualTo("Null reference error incident");
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);
        assertThat(incident.getOccurrenceCount()).isEqualTo(1);
        assertThat(incident.getSeverityScore()).isEqualTo((byte) 4);
        assertThat(incident.getConfidenceScore())
                .isEqualByComparingTo("0.900");
    }

    @Test
    void aggregatesExistingIncidentWithoutDoubleCountingCurrentAnalysis() {

        LocalDateTime earlier =
                LocalDateTime.of(2026, 6, 9, 9, 0);

        Incident incident = Incident.builder()
                .incidentId("incident-1")
                .upload(upload())
                .user(user())
                .title("Null reference error incident")
                .status(IncidentStatus.OPEN)
                .rootCause("NULL_REFERENCE_ERROR")
                .severityScore((byte) 3)
                .confidenceScore(new BigDecimal("0.600"))
                .occurrenceCount(1)
                .firstSeen(earlier)
                .lastSeen(earlier)
                .build();

        Analysis current = completedAnalysis(
                1L,
                4,
                0.80,
                LocalDateTime.of(2026, 6, 10, 10, 0)
        );

        Analysis existing = completedAnalysis(
                2L,
                5,
                0.60,
                earlier
        );
        existing.setIncident(incident);

        when(incidentRepository
                .findByUploadUploadIdAndUserIdAndRootCause(
                        "upload-1",
                        1L,
                        "NULL_REFERENCE_ERROR"
                ))
                .thenReturn(Optional.of(incident));
        when(analysisRepository.findByIncidentIncidentId("incident-1"))
                .thenReturn(List.of(existing));

        service.group(current);

        assertThat(current.getIncident()).isSameAs(incident);
        assertThat(incident.getOccurrenceCount()).isEqualTo(2);
        assertThat(incident.getSeverityScore()).isEqualTo((byte) 5);
        assertThat(incident.getConfidenceScore())
                .isEqualByComparingTo("0.700");
        assertThat(incident.getFirstSeen()).isEqualTo(earlier);
    }

    @Test
    void repeatedSaveDoesNotIncreaseOccurrenceCount() {

        Incident incident = Incident.builder()
                .incidentId("incident-1")
                .upload(upload())
                .user(user())
                .title("Null reference error incident")
                .status(IncidentStatus.OPEN)
                .rootCause("NULL_REFERENCE_ERROR")
                .severityScore((byte) 4)
                .confidenceScore(new BigDecimal("0.900"))
                .occurrenceCount(1)
                .firstSeen(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .build();

        Analysis current = completedAnalysis(
                1L,
                4,
                0.90,
                LocalDateTime.now()
        );
        current.setIncident(incident);

        when(incidentRepository
                .findByUploadUploadIdAndUserIdAndRootCause(
                        "upload-1",
                        1L,
                        "NULL_REFERENCE_ERROR"
                ))
                .thenReturn(Optional.of(incident));
        when(analysisRepository.findByIncidentIncidentId("incident-1"))
                .thenReturn(List.of(current));

        service.group(current);

        assertThat(incident.getOccurrenceCount()).isEqualTo(1);
    }

    @Test
    void reopensClosedIncidentWhenNewMatchingAnalysisArrives() {

        Incident incident = Incident.builder()
                .incidentId("incident-1")
                .upload(upload())
                .user(user())
                .title("Null reference error incident")
                .status(IncidentStatus.CLOSED)
                .rootCause("NULL_REFERENCE_ERROR")
                .severityScore((byte) 4)
                .confidenceScore(new BigDecimal("0.900"))
                .occurrenceCount(1)
                .firstSeen(LocalDateTime.now())
                .lastSeen(LocalDateTime.now())
                .build();

        Analysis current = completedAnalysis(
                1L,
                4,
                0.90,
                LocalDateTime.now()
        );

        when(incidentRepository
                .findByUploadUploadIdAndUserIdAndRootCause(
                        "upload-1",
                        1L,
                        "NULL_REFERENCE_ERROR"
                ))
                .thenReturn(Optional.of(incident));
        when(analysisRepository.findByIncidentIncidentId("incident-1"))
                .thenReturn(List.of());

        service.group(current);

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.OPEN);

        verify(historyRepository).save(any(IncidentStatusHistory.class));
        verify(eventPublisher).publishEvent(any(IncidentStatusChangedEvent.class));
    }

    @Test
    void ignoresIncompleteAnalysis() {

        Analysis analysis = completedAnalysis(
                1L,
                4,
                0.90,
                LocalDateTime.now()
        );
        analysis.setAnalysisStatus(Analysis.AnalysisStatus.PROCESSING);

        service.group(analysis);

        verify(incidentRepository, never()).save(any());
    }

    private Analysis completedAnalysis(
            Long id,
            int severity,
            double confidence,
            LocalDateTime timestamp
    ) {
        return Analysis.builder()
                .id(id)
                .upload(upload())
                .user(user())
                .rootCause("NULL_REFERENCE_ERROR")
                .summary("Null pointer in auth flow")
                .severityScore(SeverityScoreMapper.toEntityValue(severity))
                .confidenceScore(
                        ConfidenceScoreMapper.toEntityValue(confidence)
                )
                .analysisStatus(Analysis.AnalysisStatus.COMPLETED)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .build();
    }

    private Upload upload() {
        return Upload.builder()
                .uploadId("upload-1")
                .user(user())
                .build();
    }

    private User user() {
        return User.builder()
                .id(1L)
                .username("user")
                .build();
    }
}
