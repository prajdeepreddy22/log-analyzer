package com.loganalyzer.parser;

import com.loganalyzer.entity.Log;
import com.loganalyzer.service.HashKeyService;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogParserServiceTest {

    private final LogParserService parser =
            new LogParserService(new HashKeyService());

    @Test
    void parsesStackTraceAsSingleRawLogBlockAndKeepsUnknownFallback() throws Exception {

        String logs = """
                2024-01-15 10:23:45.123 ERROR 12345 --- [main] com.app.UserService : NullPointerException
                \tat com.app.UserService.getUser(UserService.java:45)
                Caused by: java.lang.NullPointerException: User is null
                \tat com.app.UserRepository.find(UserRepository.java:12)
                Some random unstructured log line
                """;

        List<ParsedLogEntry> result = parser.parse(
                new ByteArrayInputStream(logs.getBytes(StandardCharsets.UTF_8))
        );

        assertThat(result).hasSize(2);

        ParsedLogEntry error = result.get(0);
        assertThat(error.getLevel()).isEqualTo(Log.LogLevel.ERROR);
        assertThat(error.isHasStackTrace()).isTrue();
        assertThat(error.getRawLog()).contains("UserService.java:45");
        assertThat(error.getHashKey()).hasSize(64);

        ParsedLogEntry unknown = result.get(1);
        assertThat(unknown.getTimestamp()).isNull();
        assertThat(unknown.getLevel()).isEqualTo(Log.LogLevel.UNKNOWN);
        assertThat(unknown.getMessage()).isEqualTo("Some random unstructured log line");
    }
}
