package com.loganalyzer.service.impl;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.dto.response.LogResponse;
import com.loganalyzer.dto.response.LogStatsResponse;
import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.entity.Log;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.specification.LogSpecification;
import com.loganalyzer.service.LogQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogQueryServiceImpl implements LogQueryService {

    private final LogRepository logRepository;
    private final UploadRepository uploadRepository;

    private static final List<String> ALLOWED_SORT_FIELDS =
            List.of("logSequence", "logTimestamp", "level", "id");

    @Override
    public PageResponse<LogResponse> getLogs(
            String uploadId,
            Long userId,
            Pageable pageable
    ) {
        validateUpload(uploadId, userId);

        Page<Log> logs = logRepository.findByUploadUploadId(uploadId, pageable);

        return PageResponse.from(logs.map(this::mapToResponse));
    }

    @Override
    public PageResponse<LogResponse> searchLogs(
            String uploadId,
            Long userId,
            LogFilterRequest request,
            Pageable pageable
    ) {
        validateUpload(uploadId, userId);

        Specification<Log> spec = LogSpecification.build(uploadId, request);

        Page<Log> logs = logRepository.findAll(spec, pageable);

        return PageResponse.from(logs.map(this::mapToResponse));
    }

    @Override
    public LogStatsResponse getLogStats(String uploadId, Long userId) {

        validateUpload(uploadId, userId);

        return LogStatsResponse.builder()
                .totalLogs(logRepository.countByUploadUploadId(uploadId))
                .errorCount(logRepository.countByUploadUploadIdAndLevel(uploadId, Log.LogLevel.ERROR))
                .warnCount(logRepository.countByUploadUploadIdAndLevel(uploadId, Log.LogLevel.WARN))
                .infoCount(logRepository.countByUploadUploadIdAndLevel(uploadId, Log.LogLevel.INFO))
                .debugCount(logRepository.countByUploadUploadIdAndLevel(uploadId, Log.LogLevel.DEBUG))
                .build();
    }

    // FOR AI
    public List<Log> getLogsForAnalysis(String uploadId, Long userId) {

        validateUpload(uploadId, userId); // SECURITY CHECK

        List<Log.LogLevel> levels = List.of(
                Log.LogLevel.ERROR,
                Log.LogLevel.WARN
        );

        Page<Log> logs = logRepository.findByUploadUploadIdAndLevelIn(
                uploadId,
                levels,
                PageRequest.of(0, 100, Sort.by("logTimestamp").ascending())
        );

        return logs.getContent();
    }

    private void validateUpload(String uploadId, Long userId) {
        uploadRepository.findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload not found"));
    }

    private LogResponse mapToResponse(Log log) {
        return LogResponse.builder()
                .id(log.getId())
                .logSequence(log.getLogSequence())
                .level(log.getLevel() != null ? log.getLevel().name() : null)
                .message(log.getMessage())
                .serviceName(log.getServiceName())
                .hostName(log.getHostName())
                .environment(log.getEnvironment())
                .source(log.getSource() != null ? log.getSource().name() : null)
                .logTimestamp(log.getLogTimestamp())
                .build();
    }
}