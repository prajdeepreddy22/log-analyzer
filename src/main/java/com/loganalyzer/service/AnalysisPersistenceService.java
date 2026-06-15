package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisPersistenceService {

    private final AnalysisRepository analysisRepository;
    private final RootCauseNormalizer rootCauseNormalizer;
    private final ConfidenceScoreService confidenceScoreService;
    private final IncidentGroupingService incidentGroupingService;

    @Transactional
    public Analysis save(Analysis analysis) {

        String rawRootCause = analysis.getRootCause();

        if (rawRootCause != null) {
            String normalizedRootCause =
                    rootCauseNormalizer.normalize(rawRootCause);

            if (!rootCauseNormalizer.isAllowed(normalizedRootCause)) {
                log.warn(
                        "Root cause remained invalid after normalization; "
                                + "using UNKNOWN_ERROR value={}",
                        abbreviate(rawRootCause)
                );

                normalizedRootCause =
                        RootCauseNormalizer.RootCauseCategory.UNKNOWN_ERROR.name();
            }

            analysis.setRootCause(normalizedRootCause);
        }

        analysis.setConfidenceScore(
                confidenceScoreService.clamp(
                        analysis.getConfidenceScore()
                )
        );

        incidentGroupingService.group(analysis);

        return analysisRepository.save(analysis);
    }

    private String abbreviate(String value) {
        return value.length() <= 200
                ? value
                : value.substring(0, 200);
    }
}
