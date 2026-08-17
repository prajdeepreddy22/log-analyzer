package com.loganalyzer.watcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WatcherStateTest {

    @TempDir
    private Path tempDir;

    @Test
    void savesAndLoadsOffsetForSameFileAndSource() throws Exception {

        Path logFile = tempDir.resolve("app.log");
        Path stateFile = tempDir.resolve(".aeip-watcher-state.json");
        Files.writeString(logFile, "existing\n");

        WatcherConfig config = new WatcherConfig(
                logFile,
                "http://localhost:8080",
                "token",
                12L,
                stateFile,
                100,
                500L
        );

        new WatcherState(logFile, 12L, 4L).save(stateFile);

        WatcherState state = WatcherState.loadOrStartAtEnd(config);

        assertThat(state.lastReadByteOffset()).isEqualTo(4L);
    }
}
