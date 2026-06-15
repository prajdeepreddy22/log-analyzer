package com.loganalyzer.service;

import com.loganalyzer.dto.response.IncidentResponse;
import com.loganalyzer.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;

    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidents(Long userId) {

        return incidentRepository
                .findByUserIdOrderBySeverityScoreDescOccurrenceCountDescLastSeenDesc(
                        userId
                )
                .stream()
                .map(incident -> IncidentResponse.builder()
                        .incidentId(incident.getIncidentId())
                        .uploadId(incident.getUpload().getUploadId())
                        .rootCause(incident.getRootCause())
                        .severityScore(
                                SeverityScoreMapper.toApiValue(
                                        incident.getSeverityScore()
                                )
                        )
                        .confidenceScore(
                                ConfidenceScoreMapper.toApiValue(
                                        incident.getConfidenceScore()
                                )
                        )
                        .occurrenceCount(incident.getOccurrenceCount())
                        .firstSeen(incident.getFirstSeen())
                        .lastSeen(incident.getLastSeen())
                        .build()
                )
                .toList();
    }
}
