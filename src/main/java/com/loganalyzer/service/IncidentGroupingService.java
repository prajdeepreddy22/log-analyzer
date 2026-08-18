package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Incident;
import com.loganalyzer.entity.Incident.IncidentStatus;
import com.loganalyzer.entity.IncidentStatusHistory;
import com.loganalyzer.event.IncidentStatusChangedEvent;
import com.loganalyzer.repository.AnalysisRepository;
import com.loganalyzer.repository.IncidentRepository;
import com.loganalyzer.repository.IncidentStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentGroupingService {

    private final IncidentRepository incidentRepository;
    private final AnalysisRepository analysisRepository;
    private final IncidentStatusHistoryRepository historyRepository;
    private final ApplicationEventPublisher eventPublisher;

    public void group(Analysis analysis) {

        if (!isGroupable(analysis)) {
            return;
        }

        Incident previousIncident = analysis.getIncident();

        Optional<Incident> existingIncident = incidentRepository
                .findByUploadUploadIdAndUserIdAndRootCause(
                        analysis.getUpload().getUploadId(),
                        analysis.getUser().getId(),
                        analysis.getRootCause()
                );

        Incident targetIncident = existingIncident.orElseGet(() ->
                incidentRepository.save(createIncident(analysis))
        );

        reopenClosedIncidentIfNeeded(targetIncident, analysis);

        analysis.setIncident(targetIncident);

        recalculate(targetIncident, analysis, true);

        if (previousIncident != null
                && !previousIncident.getIncidentId()
                .equals(targetIncident.getIncidentId())) {
            recalculate(previousIncident, analysis, false);
        }

        log.info(
                "Analysis grouped incidentId={} uploadId={} rootCause={}",
                targetIncident.getIncidentId(),
                analysis.getUpload().getUploadId(),
                analysis.getRootCause()
        );
    }

    private boolean isGroupable(Analysis analysis) {
        return analysis.getAnalysisStatus()
                == Analysis.AnalysisStatus.COMPLETED
                && analysis.getUpload() != null
                && analysis.getUser() != null
                && analysis.getRootCause() != null
                && !analysis.getRootCause().isBlank();
    }

    private Incident createIncident(Analysis analysis) {
        LocalDateTime now = LocalDateTime.now();

        return Incident.builder()
                .incidentId(UUID.randomUUID().toString())
                .upload(analysis.getUpload())
                .user(analysis.getUser())
                .title(buildTitle(analysis.getRootCause()))
                .status(IncidentStatus.OPEN)
                .rootCause(analysis.getRootCause())
                .rootCauseSummary(truncate(analysis.getSummary(), 1000))
                .severityScore(safeSeverity(analysis.getSeverityScore()))
                .confidenceScore(safeConfidence(analysis.getConfidenceScore()))
                .occurrenceCount(1)
                .firstSeen(firstSeen(analysis, now))
                .lastSeen(now)
                .build();
    }

    private void reopenClosedIncidentIfNeeded(
            Incident incident,
            Analysis analysis
    ) {

        if (incident.getStatus() != IncidentStatus.CLOSED) {
            return;
        }

        // A fresh matching occurrence means the same production issue has
        // returned, so keep the incident identity and reopen it with history.
        IncidentStatus previousStatus = incident.getStatus();
        incident.setStatus(IncidentStatus.OPEN);

        historyRepository.save(
                IncidentStatusHistory.builder()
                        .incident(incident)
                        .fromStatus(previousStatus)
                        .toStatus(IncidentStatus.OPEN)
                        .changedBy(analysis.getUser())
                        .note("Incident reopened because a new matching occurrence was detected.")
                        .build()
        );

        eventPublisher.publishEvent(
                new IncidentStatusChangedEvent(
                        analysis.getUser().getId(),
                        incident.getIncidentId(),
                        previousStatus.name(),
                        IncidentStatus.OPEN.name()
                )
        );
    }

    private void recalculate(
            Incident incident,
            Analysis currentAnalysis,
            boolean includeCurrent
    ) {

        List<Analysis> analyses = new ArrayList<>(
                analysisRepository.findByIncidentIncidentId(
                        incident.getIncidentId()
                )
        );

        analyses.removeIf(existing ->
                sameAnalysis(existing, currentAnalysis)
        );

        if (includeCurrent) {
            analyses.add(currentAnalysis);
        }

        analyses.removeIf(analysis ->
                analysis.getAnalysisStatus()
                        != Analysis.AnalysisStatus.COMPLETED
        );

        if (analyses.isEmpty()) {
            incidentRepository.delete(incident);
            return;
        }

        incident.setOccurrenceCount(analyses.size());
        incident.setSeverityScore(
                analyses.stream()
                        .map(Analysis::getSeverityScore)
                        .filter(Objects::nonNull)
                        .map(this::safeSeverity)
                        .max(Byte::compareTo)
                        .orElse(SeverityScoreMapper.toEntityValue(1))
        );
        incident.setConfidenceScore(averageConfidence(analyses));

        LocalDateTime now = LocalDateTime.now();

        incident.setFirstSeen(
                analyses.stream()
                        .map(Analysis::getCreatedAt)
                        .filter(Objects::nonNull)
                        .min(Comparator.naturalOrder())
                        .orElse(firstSeen(currentAnalysis, now))
        );
        incident.setLastSeen(
                analyses.stream()
                        .map(Analysis::getUpdatedAt)
                        .filter(Objects::nonNull)
                        .max(Comparator.naturalOrder())
                        .orElse(now)
        );

        incidentRepository.save(incident);
    }

    private boolean sameAnalysis(
            Analysis left,
            Analysis right
    ) {
        return left.getId() != null
                && right.getId() != null
                && left.getId().equals(right.getId());
    }

    private Byte safeSeverity(Byte severityScore) {
        Byte safeValue = SeverityScoreMapper.toEntityValue(severityScore);
        return safeValue != null
                ? safeValue
                : SeverityScoreMapper.toEntityValue(1);
    }

    private BigDecimal safeConfidence(BigDecimal confidenceScore) {
        return ConfidenceScoreMapper.toEntityValue(confidenceScore);
    }

    private BigDecimal averageConfidence(List<Analysis> analyses) {
        List<BigDecimal> confidenceScores = analyses.stream()
                .map(Analysis::getConfidenceScore)
                .filter(Objects::nonNull)
                .map(this::safeConfidence)
                .toList();

        if (confidenceScores.isEmpty()) {
            return ConfidenceScoreMapper.ZERO;
        }

        BigDecimal total = confidenceScores.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ConfidenceScoreMapper.toEntityValue(
                total.divide(
                        BigDecimal.valueOf(confidenceScores.size()),
                        ConfidenceScoreMapper.SCALE,
                        RoundingMode.HALF_UP
                )
        );
    }

    private LocalDateTime firstSeen(
            Analysis analysis,
            LocalDateTime fallback
    ) {
        return analysis.getCreatedAt() != null
                ? analysis.getCreatedAt()
                : fallback;
    }

    private String buildTitle(String rootCause) {
        if (rootCause == null || rootCause.isBlank()) {
            return "Unknown incident";
        }

        String readable = rootCause
                .replace('_', ' ')
                .toLowerCase();

        return Character.toUpperCase(readable.charAt(0))
                + readable.substring(1)
                + " incident";
    }

    private String truncate(
            String value,
            int maxLength
    ) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}
