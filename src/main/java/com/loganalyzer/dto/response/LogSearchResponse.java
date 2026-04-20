package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LogSearchResponse {

    private long total;
    private int page;
    private int size;
    private List<LogResponse> logs;
}