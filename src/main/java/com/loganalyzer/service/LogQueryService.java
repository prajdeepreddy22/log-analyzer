package com.loganalyzer.service;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.dto.response.LogResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LogQueryService {

    Page<LogResponse> searchLogs(
            String uploadId,
            LogFilterRequest request,
            Pageable pageable
    );
}