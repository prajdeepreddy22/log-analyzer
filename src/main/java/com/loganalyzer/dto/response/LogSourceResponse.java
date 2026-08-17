package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LogSourceResponse {

    private Long id;
    private String sourceName;
    private String sourceType;
    private String status;
    private String internalUploadId;
    private LocalDateTime lastIngestedAt;
}
