package com.loganalyzer.parser;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedLogEntry {

    // Parsed timestamp (null if not parseable)
    private LocalDateTime timestamp;

    // Sequence to maintain order of logs
    private long logSequence;

    // Log level (default UNKNOWN)
    @Builder.Default
    private LogLevel level = LogLevel.UNKNOWN;

    // Service / class name (can be null)
    private String serviceName;

    // Full message including stack trace
    private String message;

    private String rawLog;

    // ✅ NEW
    private String exceptionType;

    // Hash key (computed later)
    private String hashKey;

    // Indicates multi-line stack trace
    @Builder.Default
    private boolean hasStackTrace = false;
}