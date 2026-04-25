package com.loganalyzer.service;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.dto.response.LogResponse;
import com.loganalyzer.dto.response.LogStatsResponse;
import com.loganalyzer.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface LogQueryService {

    PageResponse<LogResponse> getLogs(String uploadId, Long userId, Pageable pageable);

    PageResponse<LogResponse> searchLogs(
            String uploadId,
            Long userId,
            LogFilterRequest request,
            Pageable pageable
    );

    LogStatsResponse getLogStats(String uploadId, Long userId);
}