package com.loganalyzer.service;

import com.loganalyzer.dto.response.AnalysisResponse;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.AnalysisRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisServiceTest {

    @Test
    void returnsNotStartedWhenUploadExistsButAnalysisWasNotTriggered() {

        AnalysisRepository analysisRepository = mock(AnalysisRepository.class);
        UploadRepository uploadRepository = mock(UploadRepository.class);

        AnalysisService service = new AnalysisService(
                analysisRepository,
                mock(LogRepository.class),
                uploadRepository,
                mock(HashKeyService.class),
                mock(AIQueueService.class),
                mock(CacheDecisionService.class),
                mock(RateLimitService.class),
                mock(MetricsService.class)
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
}
