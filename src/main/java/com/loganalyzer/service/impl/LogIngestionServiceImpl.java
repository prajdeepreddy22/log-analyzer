package com.loganalyzer.service.impl;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.parser.LogParserService;
import com.loganalyzer.parser.ParsedLogEntry;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LogIngestionServiceImpl implements LogIngestionService {

    private final UploadRepository uploadRepository;
    private final LogRepository logRepository;
    private final StorageService storageService;
    private final LogParserService logParserService;

    @Override
    @Async
    public void process(String uploadId) {

        log.info("Starting log ingestion for uploadId={}", uploadId);

        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new RuntimeException("Upload not found"));

        try {
            // PROCESSING
            upload.setStatus(UploadStatus.PROCESSING);
            uploadRepository.save(upload);

            // Read file
            InputStream inputStream = storageService.read(upload.getFilePath());

            // Parse logs
            List<ParsedLogEntry> parsedLogs = logParserService.parse(inputStream);

            log.info("Parsed {} logs for uploadId={}", parsedLogs.size(), uploadId);

            // Convert to entity
            List<Log> logs = parsedLogs.stream()
                    .filter(entry -> entry.getTimestamp() != null)
                    .map(entry -> Log.builder()
                            .upload(upload)
                            .logTimestamp(entry.getTimestamp())
                            .logSequence(entry.getLogSequence())
                            .level(entry.getLevel()) // ✅ FIXED
                            .serviceName(entry.getServiceName())
                            .message(entry.getMessage())
                            .hashKey(entry.getHashKey())
                            .build()
                    ).toList();

            // Save
            logRepository.saveAll(logs);

            // Stats
            long errorCount = logs.stream()
                    .filter(l -> l.getLevel() == LogLevel.ERROR)
                    .count();

            long warnCount = logs.stream()
                    .filter(l -> l.getLevel() == LogLevel.WARN)
                    .count();

            upload.setTotalLogs(logs.size());
            upload.setErrorCount((int) errorCount);
            upload.setWarnCount((int) warnCount);
            upload.setStatus(UploadStatus.COMPLETED);

            uploadRepository.save(upload);

            log.info("Log ingestion completed for uploadId={}", uploadId);

        } catch (Exception e) {

            log.error("Log ingestion failed for uploadId={}", uploadId, e);

            upload.setStatus(UploadStatus.FAILED);
            uploadRepository.save(upload);
        }
    }
}