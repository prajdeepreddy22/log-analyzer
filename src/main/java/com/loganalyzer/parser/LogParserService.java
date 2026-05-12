package com.loganalyzer.parser;

import com.loganalyzer.entity.Log.LogLevel;
import com.loganalyzer.service.HashKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LogParserService {

    private final HashKeyService hashKeyService;

    // =====================================================
    // FORMAT 1 ERROR
    // =====================================================
    private static final Pattern FORMAT1 = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+" +
                    "(DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\s+" +
                    "\\[([^\\]]+)]\\s+(.+)",
            Pattern.CASE_INSENSITIVE
    );

    // =====================================================
    // FORMAT 2 ERROR
    // =====================================================
    private static final Pattern FORMAT2 = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+)\\s+" +
                    "(DEBUG|INFO|WARN|WARNING|ERROR|FATAL|SEVERE)\\s+" +
                    "\\d+\\s+---\\s+" +
                    "\\[[^\\]]+]\\s+([\\w.$]+)\\s*:\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

    // =====================================================
    // FORMAT 3 [error]
    // =====================================================
    private static final Pattern FORMAT3 = Pattern.compile(
            "^\\[(\\d{4}-\\d{2}-\\d{2} " +
                    "\\d{2}:\\d{2}:\\d{2}\\.\\d+)]\\s+" +
                    "\\[(debug|info|warn|warning|error|fatal|severe)]\\s+" +
                    "\\(([^)]+)\\)\\s+(.+)",
            Pattern.CASE_INSENSITIVE
    );

    // =====================================================
    // TIMESTAMP DETECTION
    // Supports:
    // 2026-05-05 ...
    // [2026-05-05 ...
    // =====================================================
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("^\\[?\\d{4}-\\d{2}-\\d{2}");

    private static final DateTimeFormatter FORMATTER1 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FORMATTER2 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // =====================================================
    // MAIN PARSE METHOD
    // =====================================================
    public List<ParsedLogEntry> parse(InputStream inputStream) throws IOException {

        List<ParsedLogEntry> results = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(inputStream))) {

            List<String> currentBlock = new ArrayList<>();

            long sequence = 1;

            String line;

            while ((line = reader.readLine()) != null) {

                if (line == null || line.isBlank()) {
                    continue;
                }

                // New log block detected
                if (isNewLogEntry(line) && !currentBlock.isEmpty()) {

                    results.add(parseEntry(currentBlock, sequence++));

                    currentBlock.clear();
                }

                currentBlock.add(line);
            }

            // Last block
            if (!currentBlock.isEmpty()) {
                results.add(parseEntry(currentBlock, sequence));
            }
        }

        return results;
    }

    // =====================================================
    // PARSE SINGLE ENTRY
    // =====================================================
    private ParsedLogEntry parseEntry(List<String> lines, long sequence) {

        String firstLine = lines.get(0);

        List<String> stackLines = lines.size() > 1
                ? lines.subList(1, lines.size())
                : Collections.emptyList();

        ParsedLogEntry entry = tryParseFormats(firstLine, sequence);

        entry.setRawLog(String.join("\n", lines));

        entry.setHasStackTrace(!stackLines.isEmpty());

        entry.setHashKey(hashKeyService.computeHash(entry));

        return entry;
    }

    // =====================================================
    // TRY ALL SUPPORTED FORMATS
    // =====================================================
    private ParsedLogEntry tryParseFormats(String firstLine, long sequence) {

        // FORMAT 1
        Matcher m1 = FORMAT1.matcher(firstLine);

        if (m1.find()) {

            return buildEntry(
                    m1.group(1),
                    m1.group(2),
                    m1.group(3),
                    m1.group(4),
                    sequence
            );
        }

        // FORMAT 2
        Matcher m2 = FORMAT2.matcher(firstLine);

        if (m2.find()) {

            return buildEntry(
                    m2.group(1),
                    m2.group(2),
                    m2.group(3),
                    m2.group(4),
                    sequence
            );
        }

        // FORMAT 3
        Matcher m3 = FORMAT3.matcher(firstLine);

        if (m3.find()) {

            return buildEntry(
                    m3.group(1),
                    m3.group(2),
                    m3.group(3),
                    m3.group(4),
                    sequence
            );
        }

        // UNKNOWN FORMAT
        return ParsedLogEntry.builder()
                .timestamp(null)
                .level(extractLogLevel(firstLine))
                .logSequence(sequence)
                .serviceName("UNKNOWN")
                .message(firstLine)
                .build();
    }

    // =====================================================
    // BUILD ENTRY
    // =====================================================
    private ParsedLogEntry buildEntry(
            String timestampStr,
            String levelStr,
            String service,
            String message,
            long sequence
    ) {

        return ParsedLogEntry.builder()
                .timestamp(parseTimestamp(timestampStr))
                .level(LogLevel.fromString(levelStr))
                .serviceName(service)
                .message(message)
                .logSequence(sequence)
                .build();
    }

    // =====================================================
    // DETECT LOG LEVEL GENERICALLY
    // Handles:
    // error, ERROR, Error, warn, warning etc.
    // =====================================================
    private LogLevel extractLogLevel(String line) {

        if (line == null) {
            return LogLevel.UNKNOWN;
        }

        String normalized = line.toUpperCase();

        if (normalized.contains("FATAL")
                || normalized.contains("SEVERE")) {

            return LogLevel.FATAL;
        }

        if (normalized.contains("ERROR")) {
            return LogLevel.ERROR;
        }

        if (normalized.contains("WARN")
                || normalized.contains("WARNING")) {

            return LogLevel.WARN;
        }

        if (normalized.contains("INFO")) {
            return LogLevel.INFO;
        }

        if (normalized.contains("DEBUG")) {
            return LogLevel.DEBUG;
        }

        return LogLevel.UNKNOWN;
    }

    // =====================================================
    // NEW LOG ENTRY CHECK
    // =====================================================
    private boolean isNewLogEntry(String line) {

        return TIMESTAMP_PATTERN.matcher(line).find()
                || !isStackTraceLine(line);
    }

    // =====================================================
    // STACK TRACE DETECTION
    // =====================================================
    private boolean isStackTraceLine(String line) {

        return line.startsWith("\tat ")
                || line.startsWith("Caused by:")
                || line.matches("^\\s+at .+")
                || line.matches("^\\s+\\.\\.\\. \\d+ more");
    }

    // =====================================================
    // TIMESTAMP PARSER
    // =====================================================
    private LocalDateTime parseTimestamp(String ts) {

        if (ts == null || ts.isBlank()) {
            return null;
        }

        try {

            if (ts.contains(".")) {
                return LocalDateTime.parse(ts, FORMATTER2);
            }

            return LocalDateTime.parse(ts, FORMATTER1);

        } catch (Exception e) {

            return null;
        }
    }
}