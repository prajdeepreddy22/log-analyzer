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

    private final WatcherConfig config;
    private final LogTailReader tailReader;
    private final LiveIngestionClient ingestionClient;

    public LogFileWatcher(
            WatcherConfig config,
            LogTailReader tailReader,
            LiveIngestionClient ingestionClient
    ) {
        this.config = config;
        this.tailReader = tailReader;
        this.ingestionClient = ingestionClient;
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
            boolean sent = ingestionClient.send(config, batch.lines());

            if (!sent) {
                System.err.printf(
                        "Ingestion failed for %d line(s); will retry from offset %d%n",
                        batch.lines().size(),
                        state.lastReadByteOffset()
                );
                return state;
            }

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
}
