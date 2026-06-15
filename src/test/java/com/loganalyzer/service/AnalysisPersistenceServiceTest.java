package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisPersistenceServiceTest {

    private final AnalysisRepository analysisRepository =
            mock(AnalysisRepository.class);

    private final AnalysisPersistenceService persistenceService =
            new AnalysisPersistenceService(
                    analysisRepository,
                    new RootCauseNormalizer(),
                    new ConfidenceScoreService(
                            new RootCauseNormalizer()
                    ),
                    mock(IncidentGroupingService.class)
            );

    @Test
    void normalizesRawRootCauseImmediatelyBeforeSave() {

        Analysis analysis = Analysis.builder()
                .rootCause("java.lang.NullPointerException")
                .build();

        when(analysisRepository.save(analysis)).thenReturn(analysis);

        persistenceService.save(analysis);

        ArgumentCaptor<Analysis> captor =
                ArgumentCaptor.forClass(Analysis.class);

        verify(analysisRepository).save(captor.capture());

        assertThat(captor.getValue().getRootCause())
                .isEqualTo("NULL_REFERENCE_ERROR");
    }

    @Test
    void convertsUnexpectedRootCauseToUnknownError() {

        Analysis analysis = Analysis.builder()
                .rootCause("unexpected custom AI label")
                .build();

        when(analysisRepository.save(analysis)).thenReturn(analysis);

        persistenceService.save(analysis);

        assertThat(analysis.getRootCause())
                .isEqualTo("UNKNOWN_ERROR");
    }

    @Test
    void preservesAllowedRootCause() {

        Analysis analysis = Analysis.builder()
                .rootCause("MEMORY_EXHAUSTION")
                .build();

        when(analysisRepository.save(analysis)).thenReturn(analysis);

        persistenceService.save(analysis);

        assertThat(analysis.getRootCause())
                .isEqualTo("MEMORY_EXHAUSTION");
    }

    @Test
    void preservesNullWhileAnalysisIsPending() {

        Analysis analysis = Analysis.builder()
                .rootCause(null)
                .build();

        when(analysisRepository.save(analysis)).thenReturn(analysis);

        persistenceService.save(analysis);

        assertThat(analysis.getRootCause()).isNull();
    }

    @Test
    void clampsConfidenceBeforeSave() {

        Analysis analysis = Analysis.builder()
                .analysisStatus(Analysis.AnalysisStatus.COMPLETED)
                .rootCause("NETWORK_TIMEOUT")
                .confidenceScore(new BigDecimal("1.4"))
                .build();

        when(analysisRepository.save(analysis)).thenReturn(analysis);

        persistenceService.save(analysis);

        assertThat(analysis.getConfidenceScore())
                .isEqualByComparingTo("1.000");
    }

    @Test
    void suppliesConfidenceForCompletedLegacyAnalysis() {

        Analysis analysis = Analysis.builder()
                .analysisStatus(Analysis.AnalysisStatus.COMPLETED)
                .rootCause("UNKNOWN_ERROR")
                .confidenceScore(null)
                .build();

        when(analysisRepository.save(analysis)).thenReturn(analysis);

        persistenceService.save(analysis);

        assertThat(analysis.getConfidenceScore())
                .isEqualByComparingTo("0.000");
    }
}
