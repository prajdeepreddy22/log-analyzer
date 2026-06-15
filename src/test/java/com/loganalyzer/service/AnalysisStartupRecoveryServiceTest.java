package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisStartupRecoveryServiceTest {

    @Test
    void marksInMemoryQueueStatesAsFailedAfterRestart() {

        AnalysisRepository repository = mock(AnalysisRepository.class);
        AnalysisStartupRecoveryService service =
                new AnalysisStartupRecoveryService(repository);

        List<Analysis.AnalysisStatus> interruptedStatuses = List.of(
                Analysis.AnalysisStatus.PENDING,
                Analysis.AnalysisStatus.PROCESSING,
                Analysis.AnalysisStatus.RETRYING
        );

        when(repository.markInterruptedAnalysesAsFailed(
                interruptedStatuses,
                Analysis.AnalysisStatus.FAILED,
                AnalysisStartupRecoveryService.RESTART_ERROR
        )).thenReturn(1);

        service.reconcileInterruptedAnalyses();

        verify(repository).markInterruptedAnalysesAsFailed(
                interruptedStatuses,
                Analysis.AnalysisStatus.FAILED,
                AnalysisStartupRecoveryService.RESTART_ERROR
        );
    }
}
