package com.loganalyzer.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileSizeFormatterServiceTest {

    private final FileSizeFormatterService formatter =
            new FileSizeFormatterService();

    @Test
    void shouldFormatSmallFilesInKbInsteadOfZeroMb() {

        assertThat(formatter.format(0L)).isEqualTo("0 B");
        assertThat(formatter.format(512L)).isEqualTo("512 B");
        assertThat(formatter.format(2048L)).isEqualTo("2 KB");
        assertThat(formatter.format(1536L)).isEqualTo("1.5 KB");
        assertThat(formatter.format(2L * 1024 * 1024)).isEqualTo("2 MB");
    }
}
