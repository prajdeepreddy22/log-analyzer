package com.loganalyzer.watcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogFileWatcherTest {

    @TempDir
    private Path tempDir;

    @Test
    void advancesOffsetOnlyAfterSuccessfulSend() throws Exception {

        Path logFile = tempDir.resolve("app.log");
        Path stateFile = tempDir.resolve(".aeip-watcher-state.json");
        Files.writeString(logFile, "line-1\n");

        WatcherConfig config = new WatcherConfig(
                logFile,
                "http://localhost:8080",
                "token",
                12L,
                stateFile,
                100,
                500L
        );
        WatcherState initialState = new WatcherState(logFile, 12L, 0L);

        LogFileWatcher failingWatcher = new LogFileWatcher(
                config,
                new LogTailReader(),
                new StubIngestionClient(false)
        );

        WatcherState failedState =
                failingWatcher.drainAvailableLines(initialState);

        assertThat(failedState.lastReadByteOffset()).isZero();

        LogFileWatcher successfulWatcher = new LogFileWatcher(
                config,
                new LogTailReader(),
                new StubIngestionClient(true)
        );

        WatcherState successfulState =
                successfulWatcher.drainAvailableLines(initialState);

        assertThat(successfulState.lastReadByteOffset())
                .isEqualTo(Files.size(logFile));
    }

    private static class StubIngestionClient extends LiveIngestionClient {

        private final boolean success;

        private StubIngestionClient(boolean success) {
            this.success = success;
        }

        @Override
        public boolean send(
                WatcherConfig config,
                List<String> lines
        ) throws IOException {
            return success;
        }
    }
}
