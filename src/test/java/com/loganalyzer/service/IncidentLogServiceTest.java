package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentLogServiceTest {

    private final IncidentLogService service = new IncidentLogService();

    @Test
    void buildsIncidentContextWhenSomeLogsHaveNullTimestamps() {

        Log unknown = Log.builder()
                .level(Log.LogLevel.UNKNOWN)
                .message("unstructured fallback line")
                .logTimestamp(null)
                .build();

        Log error = Log.builder()
                .level(Log.LogLevel.ERROR)
                .message("NullPointerException")
                .logTimestamp(LocalDateTime.parse("2024-01-15T10:23:45"))
                .build();

        List<Log> result = service.buildIncidentContext(
                List.of(unknown, error)
        );

        assertThat(result).contains(error, unknown);
    }
}
