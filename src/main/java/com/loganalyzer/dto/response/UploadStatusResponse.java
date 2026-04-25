package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadStatusResponse {

    private String uploadId;
    private String status;

    private int totalLogs;
    private int errorCount;
    private int warnCount;
}