package com.loganalyzer.service;

import com.loganalyzer.dto.request.UpdateIncidentStatusRequest;
import com.loganalyzer.dto.response.IncidentResponse;
import com.loganalyzer.dto.response.IncidentStatusHistoryResponse;
import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.entity.Incident;
import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.entity.IncidentStatusHistory;
import com.loganalyzer.entity.User;
import com.loganalyzer.event.IncidentStatusChangedEvent;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.IncidentRepository;
import com.loganalyzer.repository.IncidentStatusHistoryRepository;
import com.loganalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final IncidentStatusTransitionValidator transitionValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidents(Long userId) {

        return incidentRepository
                .findByUserIdOrderBySeverityScoreDescOccurrenceCountDescLastSeenDesc(
                        userId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentResponse> getIncidents(
            Long userId,
            IncidentStatus status,
            Pageable pageable
    ) {

        Page<Incident> incidents = status == null
                ? incidentRepository.findByUserId(userId, pageable)
                : incidentRepository.findByUserIdAndStatus(userId, status, pageable);

        return PageResponse.<IncidentResponse>builder()
                .content(incidents.getContent()
                        .stream()
                        .map(this::toResponse)
                        .toList())
                .page(incidents.getNumber())
                .size(incidents.getSize())
                .totalElements(incidents.getTotalElements())
                .totalPages(incidents.getTotalPages())
                .build();
    }

    @Transactional(readOnly = true)
    public IncidentResponse getIncident(
            Long userId,
            String incidentId
    ) {

        return toResponse(findUserIncident(userId, incidentId));
    }

    @Transactional
    public IncidentResponse updateStatus(
            Long userId,
            String incidentId,
            UpdateIncidentStatusRequest request
    ) {

        Incident incident = findUserIncident(userId, incidentId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        IncidentStatus fromStatus = incident.getStatus();
        IncidentStatus toStatus = request.getNewStatus();

        transitionValidator.validate(fromStatus, toStatus);

        if (fromStatus == toStatus) {
            return toResponse(incident);
        }

        incident.setStatus(toStatus);
        Incident savedIncident = incidentRepository.save(incident);

        historyRepository.save(
                IncidentStatusHistory.builder()
                        .incident(savedIncident)
                        .fromStatus(fromStatus)
                        .toStatus(toStatus)
                        .changedBy(user)
                        .note(normalizeNote(request.getNote()))
                        .build()
        );

        eventPublisher.publishEvent(
                new IncidentStatusChangedEvent(
                        userId,
                        savedIncident.getIncidentId(),
                        fromStatus.name(),
                        toStatus.name()
                )
        );

        return toResponse(savedIncident);
    }

    @Transactional(readOnly = true)
    public List<IncidentStatusHistoryResponse> getHistory(
            Long userId,
            String incidentId
    ) {

        findUserIncident(userId, incidentId);

        return historyRepository
                .findByIncidentIncidentIdAndIncidentUserIdOrderByChangedAtAsc(
                        incidentId,
                        userId
                )
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private Incident findUserIncident(
            Long userId,
            String incidentId
    ) {

        return incidentRepository.findByIncidentIdAndUserId(incidentId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Incident not found"));
    }

    private IncidentResponse toResponse(Incident incident) {

        return IncidentResponse.builder()
                .incidentId(incident.getIncidentId())
                .uploadId(incident.getUpload().getUploadId())
                .logSourceId(incident.getLogSource() == null
                        ? null
                        : incident.getLogSource().getId())
                .title(incident.getTitle())
                .status(incident.getStatus().name())
                .rootCause(incident.getRootCause())
                .rootCauseSummary(incident.getRootCauseSummary())
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
                .build();
    }

    private IncidentStatusHistoryResponse toHistoryResponse(
            IncidentStatusHistory history
    ) {

        return IncidentStatusHistoryResponse.builder()
                .id(history.getId())
                .incidentId(history.getIncident().getIncidentId())
                .fromStatus(history.getFromStatus() == null
                        ? null
                        : history.getFromStatus().name())
                .toStatus(history.getToStatus().name())
                .changedBy(history.getChangedBy().getId())
                .changedAt(history.getChangedAt())
                .note(history.getNote())
                .build();
    }

    private String normalizeNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }
}
