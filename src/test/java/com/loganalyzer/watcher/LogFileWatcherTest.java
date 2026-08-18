package com.loganalyzer.watcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogFileWatcherTest {

    @TempDir
    private Path tempDir;

    @Test
    void retriesWithBackoffAndAdvancesOffsetAfterSuccessfulSend()
            throws Exception {

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

        SequenceIngestionClient ingestionClient =
                new SequenceIngestionClient(false, false, true);

        List<Long> sleeps = new ArrayList<>();

        LogFileWatcher watcher = new LogFileWatcher(
                config,
                new LogTailReader(),
                ingestionClient,
                sleeps::add
        );

        WatcherState successfulState =
                watcher.drainAvailableLines(initialState);

        assertThat(successfulState.lastReadByteOffset())
                .isEqualTo(Files.size(logFile));
        assertThat(ingestionClient.attempts()).isEqualTo(3);
        assertThat(sleeps).containsExactly(1_000L, 2_000L);
    }

    @Test
    void doesNotAdvanceOffsetWhenRetryIsInterruptedAfterFailure()
            throws Exception {

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

        LogFileWatcher watcher = new LogFileWatcher(
                config,
                new LogTailReader(),
                new SequenceIngestionClient(false),
                millis -> {
                    throw new InterruptedException("stop test retry");
                }
        );

        assertThatThrownBy(() -> watcher.drainAvailableLines(initialState))
                .isInstanceOf(InterruptedException.class)
                .hasMessageContaining("stop test retry");

        assertThat(Files.exists(stateFile)).isFalse();
    }

    private static class SequenceIngestionClient extends LiveIngestionClient {

        private final Queue<Boolean> results;
        private int attempts;

        private SequenceIngestionClient(Boolean... results) {
            this.results = new ArrayDeque<>(Arrays.asList(results));
        }

        @Override
        public boolean send(
                WatcherConfig config,
                List<String> lines
        ) throws IOException {

            attempts++;

            if (results.isEmpty()) {
                return true;
            }

            return results.remove();
        }

        private int attempts() {
            return attempts;
        }
    }
}
