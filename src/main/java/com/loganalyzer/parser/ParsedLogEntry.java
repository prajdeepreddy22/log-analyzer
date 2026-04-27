package com.loganalyzer.parser;

import com.loganalyzer.entity.Log.LogLevel;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedLogEntry {

    private LocalDateTime timestamp;

    private long logSequence;

    @Builder.Default
    private LogLevel level = LogLevel.UNKNOWN;

    private String serviceName;

    private String message;

    private String rawLog;

    private String hashKey;

    @Builder.Default
    private boolean hasStackTrace = false;
}