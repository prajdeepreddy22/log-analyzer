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

    // Sequence to maintain order
    private long logSequence;

    // Log level
    @Builder.Default
    private LogLevel level = LogLevel.UNKNOWN;

    // Service name
    private String serviceName;

    // CLEAN message only (NO full log line)
    private String message;

    // Raw line (optional for debugging/audit)
    private String rawLog;

    // Hash key for deduplication
    private String hashKey;

    // Stack trace flag
    @Builder.Default
    private boolean hasStackTrace = false;
}