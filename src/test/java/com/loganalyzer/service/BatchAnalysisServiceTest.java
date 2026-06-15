package com.loganalyzer.service;

import com.loganalyzer.dto.batch.BatchAnalysisJob;
import com.loganalyzer.dto.response.AnalysisTriggerResponse;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BatchAnalysisServiceTest {

    @Test
    void reportsPartialCompletionAndSanitizesFailures() {
        AnalysisService analysisService = mock(AnalysisService.class);
        when(analysisService.analyze("ok", 1L, false))
                .thenReturn(AnalysisTriggerResponse.builder()
                        .status("PENDING")
                        .build());
        when(analysisService.analyze("bad", 1L, false))
                .thenThrow(new RuntimeException("database password"));

        BatchAnalysisService service =
                new BatchAnalysisService(analysisService);

        BatchAnalysisJob result =
                service.startBatchAnalysis(List.of("ok", "bad"), 1L);

        assertThat(result.getStatus()).isEqualTo("PARTIALLY_COMPLETED");
        assertThat(result.getProcessedUploads()).isEqualTo(2);
        assertThat(result.getFailedUploads()).isEqualTo(1);
        assertThat(result.getErrors())
                .containsEntry("bad", "Analysis could not be queued");
    }

    @Test
    void hidesAnotherUsersBatch() {
        AnalysisService analysisService = mock(AnalysisService.class);
        when(analysisService.analyze("upload-1", 1L, false))
                .thenReturn(AnalysisTriggerResponse.builder()
                        .status("PENDING")
                        .build());

        BatchAnalysisService service =
                new BatchAnalysisService(analysisService);

        BatchAnalysisJob job =
                service.startBatchAnalysis(List.of("upload-1"), 1L);

        assertThatThrownBy(() ->
                service.getBatchStatus(job.getBatchId(), 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void rejectsEmptyBatch() {
        BatchAnalysisService service =
                new BatchAnalysisService(mock(AnalysisService.class));

        assertThatThrownBy(() ->
                service.startBatchAnalysis(List.of(), 1L))
                .isInstanceOf(BadRequestException.class);
    }
}
