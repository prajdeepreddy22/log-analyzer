package com.loganalyzer.service;

import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.AnalysisRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    @Test
    void returnsNotStartedWhenUploadExistsButAnalysisWasNotTriggered() {

        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        UploadRepository uploadRepository = mock(UploadRepository.class);

        AnalysisService service = new AnalysisService(
                analysisRepository,
                mock(AnalysisPersistenceService.class),
                mock(LogRepository.class),
                uploadRepository,
                mock(HashKeyService.class),
                mock(AIQueueService.class),
                mock(CacheDecisionService.class),
                mock(RateLimitService.class),
                mock(MetricsService.class),
                mock(ConfidenceScoreService.class)
        );

        Upload upload = Upload.builder()
                .uploadId("upload-1")
                .user(User.builder().id(1L).username("raj2122").build())
                .build();

        when(uploadRepository.findByUploadIdAndUserId("upload-1", 1L))
                .thenReturn(Optional.of(upload));
        when(analysisRepository.findByUploadUploadIdAndUserId("upload-1", 1L))
                .thenReturn(Optional.empty());
        when(analysisRepository.findStatusByUploadIdAndUserId("upload-1", 1L))
                .thenReturn(Optional.empty());

        AnalysisResponse response = service.getAnalysis("upload-1", 1L);
        String status = service.getStatus("upload-1", 1L);

        assertThat(response.getUploadId()).isEqualTo("upload-1");
        assertThat(response.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(response.getAnalysisStatus()).isEqualTo("NOT_STARTED");
        assertThat(response.getMessage()).isEqualTo("Analysis has not been triggered yet");
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.isHasResult()).isFalse();
        assertThat(status).isEqualTo("NOT_STARTED");
    }

    @Test
    void includesUnknownLogsInAnalysisSelection() {

        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        UploadRepository uploadRepository = mock(UploadRepository.class);

        AnalysisService service = new AnalysisService(
                analysisRepository,
                mock(AnalysisPersistenceService.class),
                logRepository,
                uploadRepository,
                mock(HashKeyService.class),
                mock(AIQueueService.class),
                mock(CacheDecisionService.class),
                mock(RateLimitService.class),
                mock(MetricsService.class),
                mock(ConfidenceScoreService.class)
        );

        Upload upload = Upload.builder()
                .uploadId("upload-1")
                .user(User.builder().id(1L).username("raj2122").build())
                .build();

        when(uploadRepository.findByUploadIdAndUserId("upload-1", 1L))
                .thenReturn(Optional.of(upload));
        when(logRepository.findTop100ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
                eq("upload-1"),
                org.mockito.ArgumentMatchers.anyList()
        )).thenReturn(List.of());

        assertThatThrownBy(() -> service.analyze("upload-1", 1L, false))
                .isInstanceOf(ResourceNotFoundException.class);

        ArgumentCaptor<List<Log.LogLevel>> levelsCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(logRepository)
                .findTop100ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
                        eq("upload-1"),
                        levelsCaptor.capture()
                );

        assertThat(levelsCaptor.getValue()).containsExactly(
                Log.LogLevel.FATAL,
                Log.LogLevel.ERROR,
                Log.LogLevel.WARN,
                Log.LogLevel.UNKNOWN
        );
    }

    @Test
    void exposesSanitizedAnalysisFailureReason() {

        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        UploadRepository uploadRepository = mock(UploadRepository.class);

        AnalysisService service = new AnalysisService(
                analysisRepository,
                mock(AnalysisPersistenceService.class),
                mock(LogRepository.class),
                uploadRepository,
                mock(HashKeyService.class),
                mock(AIQueueService.class),
                mock(CacheDecisionService.class),
                mock(RateLimitService.class),
                mock(MetricsService.class),
                mock(ConfidenceScoreService.class)
        );

        Upload upload = Upload.builder()
                .uploadId("upload-1")
                .user(User.builder().id(1L).username("raj2122").build())
                .build();

        Analysis failed = Analysis.builder()
                .upload(upload)
                .user(upload.getUser())
                .hashKey("hash")
                .analysisStatus(Analysis.AnalysisStatus.FAILED)
                .errorMessage("AI provider request failed with status 429")
                .build();

        when(uploadRepository.findByUploadIdAndUserId("upload-1", 1L))
                .thenReturn(Optional.of(upload));
        when(analysisRepository.findByUploadUploadIdAndUserId("upload-1", 1L))
                .thenReturn(Optional.of(failed));

        AnalysisResponse response = service.getAnalysis("upload-1", 1L);

        assertThat(response.getStatus()).isEqualTo("FAILED");
        assertThat(response.getErrorMessage())
                .isEqualTo("AI provider request failed with status 429");
        assertThat(response.getMessage())
                .isEqualTo(
                        "Analysis failed: AI provider request failed with status 429"
                );
    }
}
