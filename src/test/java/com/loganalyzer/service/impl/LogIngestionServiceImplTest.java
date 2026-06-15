package com.loganalyzer.service.impl;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.service.HashKeyService;
import com.loganalyzer.service.UploadFailureService;
import com.loganalyzer.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LogIngestionServiceImplTest {

    @Test
    void savesUnknownLogsAndPersistsRawStackTraceMessage() throws Exception {

        UploadRepository uploadRepository = mock(UploadRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        StorageService storageService = mock(StorageService.class);
        UploadFailureService uploadFailureService =
                mock(UploadFailureService.class);

        LogParserService parser = new LogParserService(new HashKeyService());

        LogIngestionServiceImpl service = new LogIngestionServiceImpl(
                uploadRepository,
                logRepository,
                storageService,
                parser,
                uploadFailureService
        );

        Upload upload = Upload.builder()
                .uploadId("upload-1")
                .fileName("app.log")
                .filePath("stored/app.log")
                .uploadTime(LocalDateTime.now())
                .status(UploadStatus.UPLOADED)
                .build();

        String content = """
                2024-01-15 10:23:45.123 ERROR 12345 --- [main] com.app.UserService : NullPointerException
                \tat com.app.UserService.getUser(UserService.java:45)
                Caused by: java.lang.NullPointerException: User is null
                Some random unstructured log line
                """;

        when(uploadRepository.findById("upload-1"))
                .thenReturn(Optional.of(upload));
        when(storageService.read("stored/app.log"))
                .thenReturn(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));

        service.process("upload-1");

        ArgumentCaptor<List<Log>> logsCaptor = ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAll(logsCaptor.capture());
        verify(uploadRepository, times(2)).save(upload);

        List<Log> savedLogs = logsCaptor.getValue();
        assertThat(savedLogs).hasSize(2);
        assertThat(savedLogs)
                .anySatisfy(log -> {
                    assertThat(log.getLevel()).isEqualTo(Log.LogLevel.ERROR);
                    assertThat(log.getMessage()).contains("UserService.java:45");
                    assertThat(log.getMessage()).contains("Caused by:");
                })
                .anySatisfy(log -> {
                    assertThat(log.getLevel()).isEqualTo(Log.LogLevel.UNKNOWN);
                    assertThat(log.getLogTimestamp()).isNull();
                    assertThat(log.getMessage()).isEqualTo("Some random unstructured log line");
                });

        assertThat(upload.getStatus()).isEqualTo(UploadStatus.COMPLETED);
        assertThat(upload.getTotalLogs()).isEqualTo(2);
        assertThat(upload.getErrorCount()).isEqualTo(1);
    }

    @Test
    void persistsSanitizedFailureWhenStoredFileCannotBeRead()
            throws Exception {

        UploadRepository uploadRepository = mock(UploadRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        StorageService storageService = mock(StorageService.class);
        UploadFailureService uploadFailureService =
                mock(UploadFailureService.class);

        LogIngestionServiceImpl service = new LogIngestionServiceImpl(
                uploadRepository,
                logRepository,
                storageService,
                new LogParserService(new HashKeyService()),
                uploadFailureService
        );

        Upload upload = Upload.builder()
                .uploadId("upload-2")
                .fileName("app.log")
                .filePath("secret/path/app.log")
                .uploadTime(LocalDateTime.now())
                .status(UploadStatus.UPLOADED)
                .build();

        when(uploadRepository.findById("upload-2"))
                .thenReturn(Optional.of(upload));
        when(storageService.read("secret/path/app.log"))
                .thenThrow(new java.io.IOException("secret/path/app.log"));

        service.process("upload-2");

        verify(uploadFailureService).markFailed(
                "upload-2",
                "Stored file could not be read"
        );
    }

    @Test
    void truncatesOversizedSingleLogEntryBeforePersistence()
            throws Exception {

        UploadRepository uploadRepository = mock(UploadRepository.class);
        LogRepository logRepository = mock(LogRepository.class);
        StorageService storageService = mock(StorageService.class);
        UploadFailureService uploadFailureService =
                mock(UploadFailureService.class);

        LogIngestionServiceImpl service = new LogIngestionServiceImpl(
                uploadRepository,
                logRepository,
                storageService,
                new LogParserService(new HashKeyService()),
                uploadFailureService
        );

        Upload upload = Upload.builder()
                .uploadId("upload-large")
                .fileName("large.log")
                .filePath("stored/large.log")
                .uploadTime(LocalDateTime.now())
                .status(UploadStatus.UPLOADED)
                .build();

        String oversizedLine = "ERROR " + "x".repeat(70_000);

        when(uploadRepository.findById("upload-large"))
                .thenReturn(Optional.of(upload));
        when(storageService.read("stored/large.log"))
                .thenReturn(new ByteArrayInputStream(
                        oversizedLine.getBytes(StandardCharsets.UTF_8)
                ));

        service.process("upload-large");

        ArgumentCaptor<List<Log>> logsCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAll(logsCaptor.capture());

        String persistedMessage =
                logsCaptor.getValue().get(0).getMessage();

        assertThat(persistedMessage).hasSize(16_000);
        assertThat(persistedMessage)
                .endsWith("[Log entry truncated during ingestion]");
        assertThat(upload.getStatus()).isEqualTo(UploadStatus.COMPLETED);
    }
}
