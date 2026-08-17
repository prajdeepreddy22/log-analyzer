package com.loganalyzer.watcher;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LogTailReader {

    public List<LineBatch> readBatches(
            Path file,
            long offset,
            int batchSize
    ) throws IOException {
        List<LineBatch> batches = new ArrayList<>();

        try (RandomAccessFile randomAccessFile =
                     new RandomAccessFile(file.toFile(), "r")) {

            long fileSize = randomAccessFile.length();
            long safeOffset = Math.min(Math.max(offset, 0L), fileSize);

            randomAccessFile.seek(safeOffset);

            List<String> lines = new ArrayList<>();
            long lastOffset = safeOffset;
            String line;

            while ((line = randomAccessFile.readLine()) != null) {
                lines.add(toUtf8(line));
                lastOffset = randomAccessFile.getFilePointer();

                if (lines.size() == batchSize) {
                    batches.add(new LineBatch(List.copyOf(lines), lastOffset));
                    lines.clear();
                }
            }

            if (!lines.isEmpty()) {
                batches.add(new LineBatch(List.copyOf(lines), lastOffset));
            }
        }

        return batches;
    }

    private String toUtf8(String value) {
        return new String(
                value.getBytes(StandardCharsets.ISO_8859_1),
                StandardCharsets.UTF_8
        );
    }

    public record LineBatch(
            List<String> lines,
            long nextOffset
    ) {
    }
}
