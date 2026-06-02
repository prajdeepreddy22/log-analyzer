package com.loganalyzer.service.impl;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.service.HashKeyService;
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

        LogParserService parser = new LogParserService(new HashKeyService());

        LogIngestionServiceImpl service = new LogIngestionServiceImpl(
                uploadRepository,
                logRepository,
                storageService,
                parser
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
}
