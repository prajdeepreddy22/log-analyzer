package com.loganalyzer.watcher;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public record WatcherConfig(
        Path file,
        String backendUrl,
        String apiToken,
        Long sourceId,
        Path stateFile,
        int batchSize,
        long pollMillis
) {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final long DEFAULT_POLL_MILLIS = 500L;

    public static WatcherConfig fromArgs(String[] args) {
        Map<String, String> values = parseArgs(args);

        Path file = Path.of(required(values, "file"));
        Long sourceId = Long.valueOf(required(values, "source-id"));

        Path stateFile = values.containsKey("state-file")
                ? Path.of(values.get("state-file"))
                : Path.of(".aeip-watcher-state.json");

        return new WatcherConfig(
                file,
                required(values, "backend-url"),
                required(values, "token"),
                sourceId,
                stateFile,
                parseInt(values.get("batch-size"), DEFAULT_BATCH_SIZE),
                parseLong(values.get("poll-ms"), DEFAULT_POLL_MILLIS)
        );
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> values = new HashMap<>();

        for (int index = 0; index < args.length; index++) {
            String arg = args[index];

            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException("Invalid argument: " + arg);
            }

            String key = arg.substring(2);

            if (index + 1 >= args.length || args[index + 1].startsWith("--")) {
                throw new IllegalArgumentException("Missing value for: " + arg);
            }

            values.put(key, args[++index]);
        }

        return values;
    }

    private static String required(
            Map<String, String> values,
            String key
    ) {
        String value = values.get(key);

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required --" + key);
        }

        return value;
    }

    private static int parseInt(
            String value,
            int fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        int parsed = Integer.parseInt(value);
        return Math.min(Math.max(parsed, 1), 500);
    }

    private static long parseLong(
            String value,
            long fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        return Math.max(Long.parseLong(value), 100L);
    }
}
