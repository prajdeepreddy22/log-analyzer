package com.loganalyzer.watcher;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogTailReaderTest {

    @TempDir
    private Path tempDir;

    @Test
    void readsLinesFromOffsetAndReturnsNextOffsetPerBatch() throws Exception {

        Path logFile = tempDir.resolve("app.log");
        Files.writeString(logFile, "old\nnew-1\nnew-2\n");

        List<LogTailReader.LineBatch> batches =
                new LogTailReader().readBatches(logFile, 4L, 1);

        assertThat(batches).hasSize(2);
        assertThat(batches.get(0).lines()).containsExactly("new-1");
        assertThat(batches.get(1).lines()).containsExactly("new-2");
        assertThat(batches.get(1).nextOffset())
                .isEqualTo(Files.size(logFile));
    }
}
