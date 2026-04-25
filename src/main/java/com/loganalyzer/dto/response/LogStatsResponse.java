package com.loganalyzer.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LogStatsResponse {

    private Long totalLogs;
    private Long errorCount;
    private Long warnCount;
    private Long infoCount;
    private Long debugCount;
}