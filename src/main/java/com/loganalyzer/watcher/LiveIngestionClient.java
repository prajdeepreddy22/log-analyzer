package com.loganalyzer.watcher;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

public class LiveIngestionClient {

    private final HttpClient httpClient;

    public LiveIngestionClient() {
        this(HttpClient.newHttpClient());
    }

    LiveIngestionClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public boolean send(
            WatcherConfig config,
            List<String> lines
    ) throws IOException, InterruptedException {
        String body = buildJson(config.sourceId(), lines);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(resolveIngestionUri(config.backendUrl()))
                .header("Authorization", "Bearer " + config.apiToken())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        return response.statusCode() == 200 || response.statusCode() == 202;
    }

    URI resolveIngestionUri(String backendUrl) {
        String normalized = backendUrl.endsWith("/")
                ? backendUrl.substring(0, backendUrl.length() - 1)
                : backendUrl;

        if (normalized.endsWith("/api")) {
            return URI.create(normalized + "/ingest/stream");
        }

        return URI.create(normalized + "/api/ingest/stream");
    }

    private String buildJson(
            Long sourceId,
            List<String> lines
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        builder.append("\"sourceId\":").append(sourceId).append(',');
        builder.append("\"lines\":[");

        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(lines.get(index))).append('"');
        }

        builder.append("],");
        builder.append("\"batchTimestamp\":\"")
                .append(Instant.now())
                .append("\"");
        builder.append('}');

        return builder.toString();
    }

    private String escape(String value) {
        StringBuilder builder = new StringBuilder();

        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);

            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }

        return builder.toString();
    }
}
