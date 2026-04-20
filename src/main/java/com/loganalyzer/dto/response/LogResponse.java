package com.loganalyzer.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LogResponse {

    private Long id;

    private String level;

    private String message; // clean message only

    private String serviceName;

    private String hostName;

    private String environment;

    private String source;

    private LocalDateTime logTimestamp;


}