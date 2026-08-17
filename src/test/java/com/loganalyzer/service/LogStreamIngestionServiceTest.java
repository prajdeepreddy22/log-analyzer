package com.loganalyzer.service;

import com.loganalyzer.dto.request.LiveIngestionRequest;
import com.loganalyzer.dto.response.LiveIngestionResponse;
import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.LogIngestionSource;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.User;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.repository.LogIngestionSourceRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogStreamIngestionServiceTest {

    @Test
    void duplicateBatchReturnsWithoutSavingLogs() {

        LogIngestionSourceRepository sourceRepository =
                mock(LogIngestionSourceRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        IngestionDedupService dedupService = mock(IngestionDedupService.class);

        LogStreamIngestionService service = service(
                sourceRepository,
                mock(UploadRepository.class),
                logRepository,
                dedupService
        );

        LogIngestionSource source = LogIngestionSource.builder()
                .id(12L)
                .user(User.builder().id(1L).build())
                .sourceName("App")
                .sourceType(LogIngestionSource.SourceType.WATCHER)
                .status(LogIngestionSource.SourceStatus.ACTIVE)
                .build();

        LiveIngestionRequest request = request();

        when(sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(dedupService.isDuplicate("hash")).thenReturn(true);

        LiveIngestionResponse response = service.ingest(1L, request);

        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getProcessedLines()).isZero();
        verify(logRepository, never()).saveAll(any());
    }

    @Test
    void createsInternalUploadAndStoresRealtimeLogs() {

        LogIngestionSourceRepository sourceRepository =
                mock(LogIngestionSourceRepository.class);
        UploadRepository uploadRepository = mock(UploadRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        IngestionDedupService dedupService = mock(IngestionDedupService.class);

        LogStreamIngestionService service = service(
                sourceRepository,
                uploadRepository,
                logRepository,
                dedupService
        );

        LogIngestionSource source = LogIngestionSource.builder()
                .id(12L)
                .user(User.builder().id(1L).build())
                .sourceName("School App")
                .sourceType(LogIngestionSource.SourceType.WATCHER)
                .status(LogIngestionSource.SourceStatus.ACTIVE)
                .build();

        LiveIngestionRequest request = request();

        when(sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(dedupService.isDuplicate("hash")).thenReturn(false);
        when(uploadRepository.save(any(Upload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(logRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LiveIngestionResponse response = service.ingest(1L, request);

        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.getProcessedLines()).isEqualTo(1);
        assertThat(source.getInternalUpload()).isNotNull();
        assertThat(source.getInternalUpload().getFileName())
                .isEqualTo("live-school-app.log");
        verify(dedupService).markProcessed("hash");
    }

    private LogStreamIngestionService service(
            LogIngestionSourceRepository sourceRepository,
            UploadRepository uploadRepository,
            LogRepository logRepository,
            IngestionDedupService dedupService
    ) {
        return new LogStreamIngestionService(
                sourceRepository,
                uploadRepository,
                logRepository,
                new LogParserService(
                        new HashKeyService(),
                        new SensitiveDataRedactionService()
                ),
                new SensitiveDataRedactionService(),
                dedupService,
                mock(ApplicationEventPublisher.class)
        );
    }

    private LiveIngestionRequest request() {
        LiveIngestionRequest request = new LiveIngestionRequest();
        request.setSourceId(12L);
        request.setLines(List.of(
                "2026-08-17 10:00:00 ERROR [AuthService] NullPointerException"
        ));
        request.setBatchTimestamp(Instant.parse("2026-08-17T10:00:00Z"));
        return request;
    }
}
