package com.loganalyzer.service.impl;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.parser.ParsedLogEntry;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.SensitiveDataRedactionService;
import com.loganalyzer.service.UploadFailureService;
import com.loganalyzer.storage.StorageException;
import com.loganalyzer.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogIngestionServiceImpl implements LogIngestionService {

    private static final int MAX_PERSISTED_MESSAGE_CHARACTERS = 16_000;
    private static final String TRUNCATION_MARKER =
            "\n[Log entry truncated during ingestion]";

    private final UploadRepository uploadRepository;
    private final LogRepository logRepository;
    private final StorageService storageService;
    private final LogParserService logParserService;
    private final UploadFailureService uploadFailureService;
    private final SensitiveDataRedactionService sensitiveDataRedactionService;

    @Override
    @Async
    public void process(String uploadId) {

        log.info("Starting log ingestion for uploadId={}", uploadId);

        String storedFilePath = null;

        try {
            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Upload not found"));

            storedFilePath = upload.getFilePath();

            upload.setStatus(UploadStatus.PROCESSING);
            upload.setProcessingError(null);
            uploadRepository.save(upload);

            List<ParsedLogEntry> parsedLogs;
            try (InputStream inputStream =
                         storageService.read(upload.getFilePath())) {
                parsedLogs = logParserService.parse(inputStream);
            }

            log.info(
                    "Parsed {} logs for uploadId={}",
                    parsedLogs.size(),
                    uploadId
            );

            List<Log> logs = parsedLogs.stream()
                    .map(entry -> Log.builder()
                            .upload(upload)
                            .logTimestamp(entry.getTimestamp())
                            .logSequence(entry.getLogSequence())
                            .level(entry.getLevel())
                            .serviceName(entry.getServiceName())
                            .message(resolvePersistedMessage(entry))
                            .hashKey(entry.getHashKey())
                            .build()
                    )
                    .toList();

            logRepository.saveAll(logs);

            long errorCount = logs.stream()
                    .filter(item -> item.getLevel() == LogLevel.ERROR)
                    .count();

            long warnCount = logs.stream()
                    .filter(item -> item.getLevel() == LogLevel.WARN)
                    .count();

            upload.setTotalLogs(logs.size());
            upload.setErrorCount((int) errorCount);
            upload.setWarnCount((int) warnCount);
            upload.setStatus(UploadStatus.COMPLETED);
            upload.setProcessingError(null);
            uploadRepository.save(upload);

            log.info("Log ingestion completed for uploadId={}", uploadId);

        } catch (Exception exception) {
            log.error(
                    "Log ingestion failed uploadId={} type={}",
                    uploadId,
                    exception.getClass().getSimpleName()
            );
            persistFailure(uploadId, exception);
        } finally {
            cleanupStoredFile(storedFilePath, uploadId);
        }
    }

    private void persistFailure(String uploadId, Exception exception) {

        try {
            uploadFailureService.markFailed(
                    uploadId,
                    safeProcessingError(exception)
            );
        } catch (Exception persistenceException) {
            log.error(
                    "Unable to persist ingestion failure uploadId={}",
                    uploadId,
                    persistenceException
            );
        }
    }

    private String safeProcessingError(Exception exception) {

        if (exception instanceof ResourceNotFoundException) {
            return "Upload record was not found";
        }

        if (exception instanceof IOException
                || exception instanceof StorageException) {
            return "Stored file could not be read";
        }

        return "Log ingestion failed";
    }

    private String resolvePersistedMessage(ParsedLogEntry entry) {

        String message;

        if (entry.getRawLog() != null && !entry.getRawLog().isBlank()) {
            message = entry.getRawLog();
        } else {
            message = entry.getMessage();
        }

        if (message == null
                || message.length() <= MAX_PERSISTED_MESSAGE_CHARACTERS) {
            return sensitiveDataRedactionService.redact(message);
        }

        int retainedLength =
                MAX_PERSISTED_MESSAGE_CHARACTERS - TRUNCATION_MARKER.length();

        return sensitiveDataRedactionService.redact(
                message.substring(0, retainedLength) + TRUNCATION_MARKER
        );
    }

    private void cleanupStoredFile(String filePath, String uploadId) {

        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            storageService.delete(filePath);
        } catch (Exception cleanupException) {
            log.warn(
                    "Stored upload file cleanup failed after ingestion uploadId={} type={}",
                    uploadId,
                    cleanupException.getClass().getSimpleName()
            );
        }
    }
}
