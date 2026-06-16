package com.loganalyzer.service.impl;

import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.dto.response.UploadResponse;
import com.loganalyzer.dto.response.UploadStatusResponse;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.repository.UserRepository;
import com.loganalyzer.service.FileSizeFormatterService;
import com.loganalyzer.service.FileValidationService;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.MetricsService;
import com.loganalyzer.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UploadServiceImplTest {

    @Test
    void userUploadHistoryIncludesPersistedLogCounts() {

        UploadRepository uploadRepository =
                mock(UploadRepository.class);

        UploadServiceImpl service =
                service(uploadRepository);

        Upload upload = Upload.builder()
                .uploadId("upload-1")
                .fileName("sample.log")
                .filePath("stored/sample.log")
                .fileSize(2048L)
                .uploadTime(LocalDateTime.parse("2026-06-16T10:00:00"))
                .status(UploadStatus.COMPLETED)
                .totalLogs(8)
                .errorCount(2)
                .warnCount(3)
                .build();

        PageRequest pageable =
                PageRequest.of(0, 20);

        when(uploadRepository.findByUserIdOrderByUploadTimeDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(upload), pageable, 1));

        PageResponse<UploadResponse> response =
                service.getUserUploads(1L, null, pageable);

        assertThat(response.getContent()).singleElement()
                .satisfies(item -> {
                    assertThat(item.getUploadId()).isEqualTo("upload-1");
                    assertThat(item.getTotalLogs()).isEqualTo(8);
                    assertThat(item.getErrorCount()).isEqualTo(2);
                    assertThat(item.getWarnCount()).isEqualTo(3);
                    assertThat(item.getFileSizeFormatted()).isEqualTo("2 KB");
                });
    }

    @Test
    void uploadStatusDefaultsNullCountsToZero() {

        UploadRepository uploadRepository =
                mock(UploadRepository.class);

        UploadServiceImpl service =
                service(uploadRepository);

        Upload upload = Upload.builder()
                .uploadId("upload-2")
                .fileName("legacy.log")
                .filePath("stored/legacy.log")
                .fileSize(1024L)
                .uploadTime(LocalDateTime.parse("2026-06-16T10:00:00"))
                .status(UploadStatus.COMPLETED)
                .totalLogs(null)
                .errorCount(null)
                .warnCount(null)
                .build();

        when(uploadRepository.findByUploadIdAndUserId("upload-2", 1L))
                .thenReturn(Optional.of(upload));

        UploadStatusResponse response =
                service.getUploadStatus("upload-2", 1L);

        assertThat(response.getTotalLogs()).isZero();
        assertThat(response.getErrorCount()).isZero();
        assertThat(response.getWarnCount()).isZero();
    }

    private UploadServiceImpl service(UploadRepository uploadRepository) {
        return new UploadServiceImpl(
                uploadRepository,
                mock(UserRepository.class),
                mock(StorageService.class),
                mock(FileValidationService.class),
                new FileSizeFormatterService(),
                mock(LogIngestionService.class),
                mock(MetricsService.class)
        );
    }
}
