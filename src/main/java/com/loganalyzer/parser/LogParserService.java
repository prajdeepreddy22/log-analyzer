package com.loganalyzer.parser;

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

    private static final Pattern FORMAT1 = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})\\s+" +
                    "(DEBUG|INFO|WARN|ERROR|FATAL)\\s+\\[([^\\]]+)]\\s+(.+)"
    );

    private static final Pattern FORMAT2 = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d+)\\s+" +
                    "(DEBUG|INFO|WARN|ERROR|FATAL)\\s+\\d+\\s+---\\s+" +
                    "\\[[^\\]]+]\\s+([\\w.$]+)\\s*:\\s*(.+)"
    );

    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"
    );

    private static final DateTimeFormatter FORMATTER1 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter FORMATTER2 =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    public List<ParsedLogEntry> parse(InputStream inputStream) throws IOException {

        List<ParsedLogEntry> results = new ArrayList<>();

        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(inputStream))) {

            List<String> currentBlock = new ArrayList<>();
            long sequence = 1;

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) continue;

                if (isNewLogEntry(line) && !currentBlock.isEmpty()) {
                    results.add(parseEntry(currentBlock, sequence++));
                    currentBlock.clear();
                }

                currentBlock.add(line);
            }

            if (!currentBlock.isEmpty()) {
                results.add(parseEntry(currentBlock, sequence));
            }
        }

        return results;
    }

    private ParsedLogEntry parseEntry(List<String> lines, long sequence) {

        String firstLine = lines.get(0);

        List<String> stackLines = lines.size() > 1
                ? lines.subList(1, lines.size())
                : Collections.emptyList();

        ParsedLogEntry entry = tryParseFormats(firstLine, sequence);

        String fullMessage = firstLine +
                (stackLines.isEmpty() ? "" : "\n" + String.join("\n", stackLines));

        entry.setMessage(fullMessage);
        entry.setHasStackTrace(!stackLines.isEmpty());

        entry.setHashKey(hashKeyService.computeHash(entry));

        return entry;
    }

    private ParsedLogEntry tryParseFormats(String firstLine, long sequence) {

        Matcher m1 = FORMAT1.matcher(firstLine);
        if (m1.find()) {
            return buildEntry(m1.group(1), m1.group(2), m1.group(3), sequence);
        }

        Matcher m2 = FORMAT2.matcher(firstLine);
        if (m2.find()) {
            return buildEntry(m2.group(1), m2.group(2), m2.group(3), sequence);
        }

        return ParsedLogEntry.builder()
                .timestamp(null)
                .level(LogLevel.UNKNOWN)
                .logSequence(sequence)
                .serviceName(null)
                .build();
    }

    private ParsedLogEntry buildEntry(
            String timestampStr,
            String levelStr,
            String service,
            long sequence
    ) {
        return ParsedLogEntry.builder()
                .timestamp(parseTimestamp(timestampStr))
                .level(LogLevel.fromString(levelStr))
                .serviceName(service)
                .logSequence(sequence)
                .build();
    }

    private boolean isNewLogEntry(String line) {

        if (TIMESTAMP_PATTERN.matcher(line).find()) {
            return true;
        }

        if (isStackTraceLine(line)) {
            return false;
        }

        return true;
    }

    private boolean isStackTraceLine(String line) {
        return line.startsWith("\tat ") ||
                line.startsWith("Caused by:") ||
                line.startsWith("\t...") ||
                line.matches("^\\s+at .+") ||
                line.matches("^\\s+\\.\\.\\. \\d+ more");
    }

    private LocalDateTime parseTimestamp(String ts) {
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