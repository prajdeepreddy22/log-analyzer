package com.loganalyzer.service;

import com.loganalyzer.dto.request.LiveIngestionRequest;
import com.loganalyzer.dto.response.LiveIngestionResponse;
import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.LogIngestionSource;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.event.LogIngestedEvent;
import com.loganalyzer.exception.ConflictException;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.parser.ParsedLogEntry;
import com.loganalyzer.repository.LogIngestionSourceRepository;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogStreamIngestionService {

    private static final int MAX_PERSISTED_MESSAGE_CHARACTERS = 16_000;
    private static final String TRUNCATION_MARKER =
            "\n[Log entry truncated during live ingestion]";

    private final LogIngestionSourceRepository sourceRepository;
    private final UploadRepository uploadRepository;
    private final LogRepository logRepository;
    private final LogParserService logParserService;
    private final SensitiveDataRedactionService sensitiveDataRedactionService;
    private final IngestionDedupService ingestionDedupService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LiveIngestionResponse ingest(
            Long userId,
            LiveIngestionRequest request
    ) {

        LogIngestionSource source = sourceRepository
                .findByIdAndUserId(request.getSourceId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log source not found"));

        if (source.getStatus() != LogIngestionSource.SourceStatus.ACTIVE) {
            throw new ConflictException("Log source is inactive");
        }

        String batchHash = ingestionDedupService.computeBatchHash(userId, request);

        if (!ingestionDedupService.claimBatch(batchHash)) {
            return LiveIngestionResponse.builder()
                    .sourceId(source.getId())
                    .acceptedLines(0)
                    .processedLines(0)
                    .duplicate(true)
                    .uploadId(resolveUploadId(source))
                    .message("Duplicate batch ignored")
                    .build();
        }

        try {
            Upload upload = resolveInternalUpload(source);
            List<ParsedLogEntry> parsedLogs = parseLines(request.getLines());

            List<Log> logs = parsedLogs.stream()
                    .map(entry -> toLog(upload, entry))
                    .toList();

            logRepository.saveAll(logs);
            updateUploadCounters(upload, logs);

            source.setLastIngestedAt(LocalDateTime.now());
            sourceRepository.save(source);

            completeDedupAfterCommit(batchHash);

            eventPublisher.publishEvent(
                    new LogIngestedEvent(userId, source.getId(), logs.size())
            );

            log.info(
                    "Live ingestion accepted userId={} sourceId={} uploadId={} count={}",
                    userId,
                    source.getId(),
                    upload.getUploadId(),
                    logs.size()
            );

            return LiveIngestionResponse.builder()
                    .sourceId(source.getId())
                    .acceptedLines(request.getLines().size())
                    .processedLines(logs.size())
                    .duplicate(false)
                    .uploadId(upload.getUploadId())
                    .message("Log batch accepted")
                    .build();
        } catch (RuntimeException exception) {
            ingestionDedupService.releaseBatch(batchHash);
            throw exception;
        }
    }

    private List<ParsedLogEntry> parseLines(List<String> lines) {
        try {
            byte[] content = String.join(System.lineSeparator(), lines)
                    .getBytes(StandardCharsets.UTF_8);

            return logParserService.parse(new ByteArrayInputStream(content));
        } catch (Exception exception) {
            throw new ConflictException("Log batch could not be parsed");
        }
    }

    private void completeDedupAfterCommit(String batchHash) {

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            ingestionDedupService.markProcessed(batchHash);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        ingestionDedupService.markProcessed(batchHash);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            ingestionDedupService.releaseBatch(batchHash);
                        }
                    }
                }
        );
    }

    private Upload resolveInternalUpload(LogIngestionSource source) {

        if (source.getInternalUpload() != null) {
            return source.getInternalUpload();
        }

        Upload upload = uploadRepository.save(
                Upload.builder()
                        .uploadId(UUID.randomUUID().toString())
                        .user(source.getUser())
                        .fileName("live-" + sanitizeFileName(source.getSourceName()) + ".log")
                        .filePath("live-ingestion://source/" + source.getId())
                        .fileSize(0L)
                        .uploadTime(LocalDateTime.now())
                        .totalLogs(0)
                        .errorCount(0)
                        .warnCount(0)
                        .status(UploadStatus.COMPLETED)
                        .processingError(null)
                        .build()
        );

        source.setInternalUpload(upload);
        sourceRepository.save(source);

        return upload;
    }

    private String resolveUploadId(LogIngestionSource source) {
        return source.getInternalUpload() == null
                ? null
                : source.getInternalUpload().getUploadId();
    }

    private Log toLog(Upload upload, ParsedLogEntry entry) {
        return Log.builder()
                .upload(upload)
                .logTimestamp(entry.getTimestamp())
                .logSequence(nextSequence(upload, entry))
                .level(entry.getLevel())
                .serviceName(entry.getServiceName())
                .message(resolvePersistedMessage(entry))
                .source(Log.LogSource.REALTIME)
                .hashKey(entry.getHashKey())
                .build();
    }

    private Long nextSequence(Upload upload, ParsedLogEntry entry) {
        int existingTotal = upload.getTotalLogs() == null ? 0 : upload.getTotalLogs();
        return existingTotal + entry.getLogSequence();
    }

    private void updateUploadCounters(Upload upload, List<Log> logs) {

        int previousTotal = upload.getTotalLogs() == null ? 0 : upload.getTotalLogs();
        int previousErrors = upload.getErrorCount() == null ? 0 : upload.getErrorCount();
        int previousWarns = upload.getWarnCount() == null ? 0 : upload.getWarnCount();

        int newErrors = (int) logs.stream()
                .filter(log -> log.getLevel() == Log.LogLevel.ERROR)
                .count();

        int newWarns = (int) logs.stream()
                .filter(log -> log.getLevel() == Log.LogLevel.WARN)
                .count();

        upload.setTotalLogs(previousTotal + logs.size());
        upload.setErrorCount(previousErrors + newErrors);
        upload.setWarnCount(previousWarns + newWarns);
        upload.setStatus(UploadStatus.COMPLETED);
        upload.setProcessingError(null);
        uploadRepository.save(upload);
    }

    private String resolvePersistedMessage(ParsedLogEntry entry) {

        String message = entry.getRawLog() != null && !entry.getRawLog().isBlank()
                ? entry.getRawLog()
                : entry.getMessage();

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

    private String sanitizeFileName(String sourceName) {

        if (sourceName == null || sourceName.isBlank()) {
            return "source";
        }

        String sanitized = sourceName
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        return sanitized.isBlank() ? "source" : sanitized;
    }
}
