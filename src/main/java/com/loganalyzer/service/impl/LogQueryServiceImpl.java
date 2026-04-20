package com.loganalyzer.service.impl;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.dto.response.LogResponse;
import com.loganalyzer.entity.Log;
import com.loganalyzer.repository.LogRepository;
import com.loganalyzer.specification.LogSpecification;
import com.loganalyzer.service.LogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogQueryServiceImpl implements LogQueryService {

    private final LogRepository logRepository;

    @Override
    public Page<LogResponse> searchLogs(
            String uploadId,
            LogFilterRequest request,
            Pageable pageable
    ) {

        Specification<Log> spec = Specification.where(
                LogSpecification.hasUploadId(uploadId)
        );

        if (request.getLevel() != null) {
            spec = spec.and(LogSpecification.hasLevel(request.getLevel()));
        }

        if (request.getServiceName() != null && !request.getServiceName().isBlank()) {
            spec = spec.and(LogSpecification.hasServiceName(request.getServiceName()));
        }

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            spec = spec.and(LogSpecification.containsKeyword(request.getKeyword()));
        }

        spec = spec.and(
                LogSpecification.betweenDates(
                        request.getStartDate(),
                        request.getEndDate()
                )
        );

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("logSequence").ascending()
        );

        Page<Log> logs = logRepository.findAll(spec, sortedPageable);

        return logs.map(this::mapToResponse);
    }

    private LogResponse mapToResponse(Log log) {
        return LogResponse.builder()
                .id(log.getId())
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