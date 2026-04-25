package com.loganalyzer.dto.request;

import com.loganalyzer.entity.Log.LogLevel;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogFilterRequest {

    private LogLevel level;
    private String serviceName;
    private String keyword;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private int page = 0;
    private int size = 10;

    private String sortBy = "logSequence";
    private String sortDir = "asc";
}