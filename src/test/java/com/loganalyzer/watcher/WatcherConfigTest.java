package com.loganalyzer.watcher;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WatcherConfigTest {

    @Test
    void parsesRequiredAndOptionalArguments() {

        WatcherConfig config = WatcherConfig.fromArgs(new String[]{
                "--file", "app.log",
                "--backend-url", "http://localhost:8080",
                "--token", "token",
                "--source-id", "12",
                "--batch-size", "999",
                "--poll-ms", "10"
        });

        assertThat(config.file().toString()).isEqualTo("app.log");
        assertThat(config.backendUrl()).isEqualTo("http://localhost:8080");
        assertThat(config.apiToken()).isEqualTo("token");
        assertThat(config.sourceId()).isEqualTo(12L);
        assertThat(config.batchSize()).isEqualTo(500);
        assertThat(config.pollMillis()).isEqualTo(100L);
    }
}
