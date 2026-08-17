package com.loganalyzer.service;

import com.loganalyzer.dto.request.UpdateIncidentStatusRequest;
import com.loganalyzer.dto.response.IncidentResponse;
import com.loganalyzer.entity.Incident;
import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.entity.IncidentStatusHistory;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.User;
import com.loganalyzer.event.IncidentStatusChangedEvent;
import com.loganalyzer.repository.IncidentStatusHistoryRepository;
import com.loganalyzer.repository.IncidentRepository;
import com.loganalyzer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncidentServiceTest {

    @Test
    void mapsUserIncidentsToApiResponse() {

        IncidentRepository repository =
                mock(IncidentRepository.class);

        IncidentService service =
                new IncidentService(
                        repository,
                        mock(IncidentStatusHistoryRepository.class),
                        mock(UserRepository.class),
                        mock(IncidentStatusTransitionValidator.class),
                        mock(ApplicationEventPublisher.class)
                );

        Incident incident = Incident.builder()
                .incidentId("incident-1")
                .upload(Upload.builder()
                        .uploadId("upload-1")
                        .build())
                .title("Network timeout incident")
                .status(IncidentStatus.OPEN)
                .rootCause("NETWORK_TIMEOUT")
                .severityScore((byte) 4)
                .confidenceScore(new BigDecimal("0.800"))
                .occurrenceCount(3)
                .firstSeen(LocalDateTime.of(2026, 6, 9, 10, 0))
                .lastSeen(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();

        when(repository
                .findByUserIdOrderBySeverityScoreDescOccurrenceCountDescLastSeenDesc(
                        1L
                ))
                .thenReturn(List.of(incident));

        List<IncidentResponse> responses =
                service.getIncidents(1L);

        assertThat(responses).singleElement()
                .satisfies(response -> {
                    assertThat(response.getIncidentId())
                            .isEqualTo("incident-1");
                    assertThat(response.getUploadId())
                            .isEqualTo("upload-1");
                    assertThat(response.getOccurrenceCount())
                            .isEqualTo(3);
                });
    }

    @Test
    void updatesIncidentStatusAndWritesHistory() {

        IncidentRepository repository =
                mock(IncidentRepository.class);
        IncidentStatusHistoryRepository historyRepository =
                mock(IncidentStatusHistoryRepository.class);
        UserRepository userRepository =
                mock(UserRepository.class);
        IncidentStatusTransitionValidator transitionValidator =
                mock(IncidentStatusTransitionValidator.class);
        ApplicationEventPublisher eventPublisher =
                mock(ApplicationEventPublisher.class);

        IncidentService service =
                new IncidentService(
                        repository,
                        historyRepository,
                        userRepository,
                        transitionValidator,
                        eventPublisher
                );

        User user = User.builder()
                .id(1L)
                .username("rajdeep")
                .build();

        Incident incident = Incident.builder()
                .incidentId("incident-1")
                .upload(Upload.builder()
                        .uploadId("upload-1")
                        .build())
                .user(user)
                .title("Network timeout incident")
                .status(IncidentStatus.OPEN)
                .rootCause("NETWORK_TIMEOUT")
                .severityScore((byte) 4)
                .confidenceScore(new BigDecimal("0.800"))
                .occurrenceCount(3)
                .firstSeen(LocalDateTime.of(2026, 6, 9, 10, 0))
                .lastSeen(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();

        UpdateIncidentStatusRequest request =
                new UpdateIncidentStatusRequest();
        request.setNewStatus(IncidentStatus.INVESTIGATING);
        request.setNote("checking production logs");

        when(repository.findByIncidentIdAndUserId("incident-1", 1L))
                .thenReturn(Optional.of(incident));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
        when(repository.save(any(Incident.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse response =
                service.updateStatus(1L, "incident-1", request);

        assertThat(response.getStatus())
                .isEqualTo(IncidentStatus.INVESTIGATING.name());

        verify(transitionValidator)
                .validate(IncidentStatus.OPEN, IncidentStatus.INVESTIGATING);
        verify(historyRepository).save(any(IncidentStatusHistory.class));
        verify(eventPublisher).publishEvent(any(IncidentStatusChangedEvent.class));
    }
}
