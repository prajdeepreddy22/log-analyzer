package com.loganalyzer.service;

import com.loganalyzer.dto.response.IncidentResponse;
import com.loganalyzer.entity.Incident;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.repository.IncidentRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentServiceTest {

    @Test
    void mapsUserIncidentsToApiResponse() {

        IncidentRepository repository =
                mock(IncidentRepository.class);

        IncidentService service =
                new IncidentService(repository);

        Incident incident = Incident.builder()
                .incidentId("incident-1")
                .upload(Upload.builder()
                        .uploadId("upload-1")
                        .build())
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
}
