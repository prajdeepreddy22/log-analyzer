package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LiveIngestionResponse {

    private Long sourceId;
    private int acceptedLines;
    private int processedLines;
    private boolean duplicate;
    private String uploadId;
    private String message;
}
