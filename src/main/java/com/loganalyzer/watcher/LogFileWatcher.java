package com.loganalyzer.watcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

public class LogFileWatcher {

    private static final long INITIAL_RETRY_DELAY_MILLIS = 1_000L;
    private static final long MAX_RETRY_DELAY_MILLIS = 16_000L;

    private final WatcherConfig config;
    private final LogTailReader tailReader;
    private final LiveIngestionClient ingestionClient;
    private final RetrySleeper retrySleeper;

    public LogFileWatcher(
            WatcherConfig config,
            LogTailReader tailReader,
            LiveIngestionClient ingestionClient
    ) {
        this(
                config,
                tailReader,
                ingestionClient,
                Thread::sleep
        );
    }

    LogFileWatcher(
            WatcherConfig config,
            LogTailReader tailReader,
            LiveIngestionClient ingestionClient,
            RetrySleeper retrySleeper
    ) {
        this.config = config;
        this.tailReader = tailReader;
        this.ingestionClient = ingestionClient;
        this.retrySleeper = retrySleeper;
    }

    public void run() throws IOException, InterruptedException {
        validateFile();
        Path file = config.file();

        WatcherState state = WatcherState.loadOrStartAtEnd(config);
        state.save(config.stateFile());

        System.out.printf(
                "Watching %s from byte offset %d%n",
                config.file(),
                state.lastReadByteOffset()
        );

        try (WatchService watchService =
                     file.getFileSystem().newWatchService()) {

            Path parent = file.toAbsolutePath().getParent();

            if (parent == null) {
                throw new IOException("Unable to resolve watcher parent path");
            }

            parent.register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            state = drainAvailableLines(state);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.poll(
                        config.pollMillis(),
                        TimeUnit.MILLISECONDS
                );

                if (key == null) {
                    state = drainAvailableLines(state);
                    continue;
                }

                if (hasTargetFileEvent(key, file)) {
                    state = drainAvailableLines(state);
                }

                if (!key.reset()) {
                    throw new IOException("Watcher key is no longer valid");
                }
            }
        }
    }

    WatcherState drainAvailableLines(WatcherState state)
            throws IOException, InterruptedException {

        for (LogTailReader.LineBatch batch : tailReader.readBatches(
                config.file(),
                state.lastReadByteOffset(),
                config.batchSize()
        )) {
            sendWithRetry(batch, state.lastReadByteOffset());

            state = state.withOffset(batch.nextOffset());
            state.save(config.stateFile());

            System.out.printf(
                    "Sent %d line(s), offset=%d%n",
                    batch.lines().size(),
                    state.lastReadByteOffset()
            );
        }

        return state;
    }

    private void sendWithRetry(
            LogTailReader.LineBatch batch,
            long currentOffset
    ) throws IOException, InterruptedException {

        long delayMillis = INITIAL_RETRY_DELAY_MILLIS;

        while (!Thread.currentThread().isInterrupted()) {
            boolean sent = ingestionClient.send(config, batch.lines());

            if (sent) {
                return;
            }

            System.err.printf(
                    "Ingestion failed for %d line(s); retrying from offset %d in %d ms%n",
                    batch.lines().size(),
                    currentOffset,
                    delayMillis
            );

            retrySleeper.sleep(delayMillis);
            delayMillis = Math.min(delayMillis * 2, MAX_RETRY_DELAY_MILLIS);
        }

        throw new InterruptedException("Watcher interrupted while retrying ingestion");
    }

    private void validateFile() throws IOException {
        Path file = config.file();
        Path parent = file.toAbsolutePath().getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (!Files.exists(file)) {
            Files.createFile(file);
        }

        if (!Files.isRegularFile(file)) {
            throw new IOException("Watcher target is not a file: " + file);
        }
    }

    private boolean hasTargetFileEvent(
            WatchKey key,
            Path file
    ) {
        Path targetName = file.toAbsolutePath().getFileName();

        for (WatchEvent<?> event : key.pollEvents()) {
            if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                return true;
            }

            if (targetName != null && targetName.equals(event.context())) {
                return true;
            }
        }

        return false;
    }

    @FunctionalInterface
    interface RetrySleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
