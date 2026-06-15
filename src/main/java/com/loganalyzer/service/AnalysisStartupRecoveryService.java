package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisStartupRecoveryService {

    static final String RESTART_ERROR =
            "Analysis was interrupted by an application restart. Please retry.";

    private final AnalysisRepository analysisRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileInterruptedAnalyses() {

        int recovered = analysisRepository.markInterruptedAnalysesAsFailed(
                List.of(
                        Analysis.AnalysisStatus.PENDING,
                        Analysis.AnalysisStatus.PROCESSING,
                        Analysis.AnalysisStatus.RETRYING
                ),
                Analysis.AnalysisStatus.FAILED,
                RESTART_ERROR
        );

        if (recovered > 0) {
            log.warn(
                    "Marked {} interrupted analysis job(s) as FAILED after startup",
                    recovered
            );
        }
    }
}
