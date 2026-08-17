package com.loganalyzer.watcher;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record WatcherState(
        Path filePath,
        Long sourceId,
        long lastReadByteOffset
) {

    private static final Pattern OFFSET_PATTERN =
            Pattern.compile("\"lastReadByteOffset\"\\s*:\\s*(\\d+)");
    private static final Pattern SOURCE_PATTERN =
            Pattern.compile("\"sourceId\"\\s*:\\s*(\\d+)");
    private static final Pattern FILE_PATTERN =
            Pattern.compile("\"filePath\"\\s*:\\s*\"([^\"]*)\"");

    public static WatcherState loadOrStartAtEnd(
            WatcherConfig config
    ) throws IOException {
        Path stateFile = config.stateFile();

        if (!Files.exists(stateFile)) {
            return new WatcherState(
                    config.file(),
                    config.sourceId(),
                    Files.exists(config.file()) ? Files.size(config.file()) : 0L
            );
        }

        String json = Files.readString(stateFile, StandardCharsets.UTF_8);
        Long sourceId = matchLong(SOURCE_PATTERN, json);
        Long offset = matchLong(OFFSET_PATTERN, json);
        String filePath = matchString(FILE_PATTERN, json);

        if (sourceId == null
                || offset == null
                || filePath == null
                || !sourceId.equals(config.sourceId())
                || !Path.of(filePath).equals(config.file())) {
            return new WatcherState(
                    config.file(),
                    config.sourceId(),
                    Files.exists(config.file()) ? Files.size(config.file()) : 0L
            );
        }

        return new WatcherState(config.file(), sourceId, offset);
    }

    public void save(Path stateFile) throws IOException {
        Path parent = stateFile.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        String json = """
                {
                  "filePath": "%s",
                  "sourceId": %d,
                  "lastReadByteOffset": %d
                }
                """.formatted(
                escape(filePath.toString()),
                sourceId,
                lastReadByteOffset
        );

        Files.writeString(stateFile, json, StandardCharsets.UTF_8);
    }

    public WatcherState withOffset(long offset) {
        return new WatcherState(filePath, sourceId, offset);
    }

    private static Long matchLong(
            Pattern pattern,
            String json
    ) {
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? Long.valueOf(matcher.group(1)) : null;
    }

    private static String matchString(
            Pattern pattern,
            String json
    ) {
        Matcher matcher = pattern.matcher(json);
        return matcher.find()
                ? matcher.group(1).replace("\\\"", "\"").replace("\\\\", "\\")
                : null;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
