package com.loganalyzer.service;

import com.loganalyzer.dto.request.LiveIngestionRequest;
import com.loganalyzer.dto.response.LiveIngestionResponse;
import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.LogIngestionSource;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.entity.User;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.repository.LogIngestionSourceRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogStreamIngestionServiceTest {

    @Test
    void firstIngestionPersistsParsedLogs() {

        TestFixture fixture = fixture();
        LogIngestionSource source = activeSource(12L, internalUpload(12L));
        LiveIngestionRequest request = request(12L, standardLines());

        when(fixture.sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(fixture.dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(fixture.dedupService.claimBatch("hash")).thenReturn(true);
        saveAllReturnsArgument(fixture.logRepository);

        LiveIngestionResponse response = fixture.service.ingest(1L, request);

        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.getAcceptedLines()).isEqualTo(2);
        assertThat(response.getProcessedLines()).isEqualTo(2);
        assertThat(source.getInternalUpload().getTotalLogs()).isEqualTo(2);
        assertThat(source.getInternalUpload().getErrorCount()).isEqualTo(1);
        assertThat(source.getInternalUpload().getWarnCount()).isEqualTo(1);
        verify(fixture.logRepository).saveAll(any());
        verify(fixture.dedupService).markProcessed("hash");
    }

    @Test
    void duplicateBatchWithinWindowReturnsWithoutSavingLogs() {

        TestFixture fixture = fixture();
        LogIngestionSource source = activeSource(12L, internalUpload(12L));
        LiveIngestionRequest request = request(12L, standardLines());

        when(fixture.sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(fixture.dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(fixture.dedupService.claimBatch("hash")).thenReturn(false);

        LiveIngestionResponse response = fixture.service.ingest(1L, request);

        assertThat(response.isDuplicate()).isTrue();
        assertThat(response.getAcceptedLines()).isZero();
        assertThat(response.getProcessedLines()).isZero();
        assertThat(response.getUploadId()).isEqualTo(source.getInternalUpload().getUploadId());
        verify(fixture.logRepository, never()).saveAll(any());
        verify(fixture.dedupService, never()).markProcessed(any());
    }

    @Test
    void sameSourceWithDifferentBatchContentIsProcessed() {

        TestFixture fixture = fixture();
        LogIngestionSource source = activeSource(12L, internalUpload(12L));
        LiveIngestionRequest firstRequest = request(12L, standardLines());
        LiveIngestionRequest secondRequest = request(12L, List.of(
                "2026-08-18 19:26:10 ERROR [OrderService] SQL exception while saving order",
                "2026-08-18 19:26:12 WARN [OrderService] Retry scheduled"
        ));

        when(fixture.sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(fixture.dedupService.computeBatchHash(1L, firstRequest)).thenReturn("hash-1");
        when(fixture.dedupService.computeBatchHash(1L, secondRequest)).thenReturn("hash-2");
        when(fixture.dedupService.claimBatch("hash-1")).thenReturn(true);
        when(fixture.dedupService.claimBatch("hash-2")).thenReturn(true);
        saveAllReturnsArgument(fixture.logRepository);

        LiveIngestionResponse firstResponse = fixture.service.ingest(1L, firstRequest);
        LiveIngestionResponse secondResponse = fixture.service.ingest(1L, secondRequest);

        assertThat(firstResponse.isDuplicate()).isFalse();
        assertThat(secondResponse.isDuplicate()).isFalse();
        assertThat(firstResponse.getProcessedLines()).isEqualTo(2);
        assertThat(secondResponse.getProcessedLines()).isEqualTo(2);
        verify(fixture.logRepository, times(2)).saveAll(any());
    }

    @Test
    void sameBatchOutsideDedupWindowIsProcessedAgain() {

        TestFixture fixture = fixture();
        LogIngestionSource source = activeSource(12L, internalUpload(12L));
        LiveIngestionRequest request = request(12L, standardLines());

        when(fixture.sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(fixture.dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(fixture.dedupService.claimBatch("hash")).thenReturn(true, true);
        saveAllReturnsArgument(fixture.logRepository);

        LiveIngestionResponse firstResponse = fixture.service.ingest(1L, request);
        LiveIngestionResponse secondResponse = fixture.service.ingest(1L, request);

        assertThat(firstResponse.isDuplicate()).isFalse();
        assertThat(secondResponse.isDuplicate()).isFalse();
        assertThat(firstResponse.getProcessedLines()).isEqualTo(2);
        assertThat(secondResponse.getProcessedLines()).isEqualTo(2);
        verify(fixture.logRepository, times(2)).saveAll(any());
    }

    @Test
    void concurrentDuplicateBatchPersistsOnlyOnce() throws Exception {

        TestFixture fixture = fixture();
        LogIngestionSource source = activeSource(12L, internalUpload(12L));
        LiveIngestionRequest request = request(12L, standardLines());
        AtomicBoolean claimed = new AtomicBoolean(false);
        AtomicInteger savedLogCount = new AtomicInteger(0);

        when(fixture.sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(fixture.dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(fixture.dedupService.claimBatch("hash"))
                .thenAnswer(invocation -> claimed.compareAndSet(false, true));
        when(fixture.logRepository.saveAll(any()))
                .thenAnswer(invocation -> {
                    Iterable<Log> logs = invocation.getArgument(0);
                    savedLogCount.addAndGet(count(logs));
                    return logs;
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<LiveIngestionResponse> first =
                    executor.submit(() -> fixture.service.ingest(1L, request));
            Future<LiveIngestionResponse> second =
                    executor.submit(() -> fixture.service.ingest(1L, request));

            List<LiveIngestionResponse> responses = List.of(
                    first.get(5, TimeUnit.SECONDS),
                    second.get(5, TimeUnit.SECONDS)
            );

            assertThat(responses)
                    .extracting(LiveIngestionResponse::isDuplicate)
                    .containsExactlyInAnyOrder(false, true);
            assertThat(responses.stream()
                    .mapToInt(LiveIngestionResponse::getProcessedLines)
                    .sum()).isEqualTo(2);
            assertThat(savedLogCount.get()).isEqualTo(2);
            verify(fixture.logRepository, times(1)).saveAll(any());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void createsInternalUploadAndStoresRealtimeLogs() {

        TestFixture fixture = fixture();
        LogIngestionSource source = activeSource(12L, null);
        LiveIngestionRequest request = request(12L, List.of(
                "2026-08-17 10:00:00 ERROR [AuthService] NullPointerException"
        ));

        when(fixture.sourceRepository.findByIdAndUserId(12L, 1L))
                .thenReturn(Optional.of(source));
        when(fixture.dedupService.computeBatchHash(1L, request)).thenReturn("hash");
        when(fixture.dedupService.claimBatch("hash")).thenReturn(true);
        when(fixture.uploadRepository.save(any(Upload.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        saveAllReturnsArgument(fixture.logRepository);

        LiveIngestionResponse response = fixture.service.ingest(1L, request);

        assertThat(response.isDuplicate()).isFalse();
        assertThat(response.getProcessedLines()).isEqualTo(1);
        assertThat(source.getInternalUpload()).isNotNull();
        assertThat(source.getInternalUpload().getFileName())
                .isEqualTo("live-school-app.log");
        verify(fixture.dedupService).markProcessed("hash");
    }

    private TestFixture fixture() {

        LogIngestionSourceRepository sourceRepository =
                mock(LogIngestionSourceRepository.class);
        UploadRepository uploadRepository = mock(UploadRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        IngestionDedupService dedupService = mock(IngestionDedupService.class);

        LogStreamIngestionService service = new LogStreamIngestionService(
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

        return new TestFixture(
                service,
                sourceRepository,
                uploadRepository,
                logRepository,
                dedupService
        );
    }

    private LogIngestionSource activeSource(Long sourceId, Upload internalUpload) {

        return LogIngestionSource.builder()
                .id(sourceId)
                .user(User.builder().id(1L).build())
                .sourceName("School App")
                .sourceType(LogIngestionSource.SourceType.WATCHER)
                .status(LogIngestionSource.SourceStatus.ACTIVE)
                .internalUpload(internalUpload)
                .build();
    }

    private Upload internalUpload(Long sourceId) {

        return Upload.builder()
                .uploadId(UUID.randomUUID().toString())
                .user(User.builder().id(1L).build())
                .fileName("live-source-" + sourceId + ".log")
                .filePath("live-ingestion://source/" + sourceId)
                .fileSize(0L)
                .uploadTime(LocalDateTime.now())
                .totalLogs(0)
                .errorCount(0)
                .warnCount(0)
                .status(UploadStatus.COMPLETED)
                .build();
    }

    private LiveIngestionRequest request(Long sourceId, List<String> lines) {

        LiveIngestionRequest request = new LiveIngestionRequest();
        request.setSourceId(sourceId);
        request.setLines(lines);
        request.setBatchTimestamp(Instant.parse("2026-08-18T13:55:00Z"));
        return request;
    }

    private List<String> standardLines() {

        return List.of(
                "2026-08-18 19:25:10 ERROR [AuthService] NullPointerException while processing login",
                "2026-08-18 19:25:12 WARN [PaymentService] Payment gateway timeout"
        );
    }

    private void saveAllReturnsArgument(LogRepository logRepository) {

        when(logRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private int count(Iterable<Log> logs) {

        return (int) StreamSupport.stream(logs.spliterator(), false).count();
    }

    private record TestFixture(
            LogStreamIngestionService service,
            LogIngestionSourceRepository sourceRepository,
            UploadRepository uploadRepository,
            LogRepository logRepository,
            IngestionDedupService dedupService
    ) {
    }
}
