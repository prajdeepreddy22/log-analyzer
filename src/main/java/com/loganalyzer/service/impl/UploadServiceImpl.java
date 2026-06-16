package com.loganalyzer.service.impl;

import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.dto.response.UploadResponse;
import com.loganalyzer.dto.response.UploadStatusResponse;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.InsufficientStorageException;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.exception.ServiceUnavailableException;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.repository.UserRepository;
import com.loganalyzer.service.FileSizeFormatterService;
import com.loganalyzer.service.FileValidationService;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.MetricsService;
import com.loganalyzer.service.UploadService;
import com.loganalyzer.storage.StorageException;
import com.loganalyzer.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {

    private final UploadRepository uploadRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;
    private final FileValidationService fileValidationService;
    private final FileSizeFormatterService fileSizeFormatterService;
    private final LogIngestionService logIngestionService;
    private final MetricsService metricsService;

    @Override
    @Transactional
    public UploadResponse uploadFile(MultipartFile file, String username) {

        fileValidationService.validate(file);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + username));

        String uploadId = UUID.randomUUID().toString();
        String filePath = null;

        log.info("Starting upload: uploadId={}, user={}", uploadId, username);

        try {
            filePath = storageService.store(file, uploadId);

            Upload upload = Upload.builder()
                    .uploadId(uploadId)
                    .user(user)
                    .fileName(file.getOriginalFilename())
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .uploadTime(LocalDateTime.now())
                    .status(UploadStatus.UPLOADED)
                    .build();

            uploadRepository.save(upload);
            metricsService.getUploadCounter().increment();

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info(
                                    "Triggering async ingestion for uploadId={}",
                                    uploadId
                            );
                            logIngestionService.process(uploadId);
                        }
                    }
            );

            return UploadResponse.builder()
                    .uploadId(uploadId)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .fileSizeFormatted(fileSizeFormatterService.format(file.getSize()))
                    .status(UploadStatus.UPLOADED.name())
                    .uploadTime(upload.getUploadTime())
                    .message("File uploaded successfully. Processing started.")
                    .totalLogs(safeCount(upload.getTotalLogs()))
                    .errorCount(safeCount(upload.getErrorCount()))
                    .warnCount(safeCount(upload.getWarnCount()))
                    .build();

        } catch (IOException | StorageException e) {
            log.error(
                    "Upload storage failed uploadId={} type={}",
                    uploadId,
                    e.getClass().getSimpleName()
            );
            cleanupStoredFile(filePath, uploadId);

            if (isInsufficientStorage(e)) {
                throw new InsufficientStorageException(
                        "Insufficient file storage",
                        e
                );
            }

            throw new ServiceUnavailableException(
                    "File storage is temporarily unavailable",
                    e
            );
        } catch (DataAccessException e) {
            log.error(
                    "Upload database operation failed uploadId={} type={}",
                    uploadId,
                    e.getClass().getSimpleName()
            );
            cleanupStoredFile(filePath, uploadId);
            throw e;
        } catch (RuntimeException e) {
            log.error(
                    "Unexpected upload failure uploadId={} type={}",
                    uploadId,
                    e.getClass().getSimpleName()
            );
            cleanupStoredFile(filePath, uploadId);
            throw e;
        }
    }

    @Override
    public UploadStatusResponse getUploadStatus(String uploadId, Long userId) {

        if (uploadId == null || uploadId.isBlank()) {
            throw new BadRequestException("Invalid uploadId");
        }

        Upload upload = uploadRepository
                .findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Upload not found"));

        return UploadStatusResponse.builder()
                .uploadId(upload.getUploadId())
                .status(upload.getStatus().name())
                .totalLogs(safeCount(upload.getTotalLogs()))
                .errorCount(safeCount(upload.getErrorCount()))
                .warnCount(safeCount(upload.getWarnCount()))
                .errorMessage(upload.getProcessingError())
                .build();
    }

    @Override
    public PageResponse<UploadResponse> getUserUploads(
            Long userId,
            UploadStatus status,
            Pageable pageable
    ) {

        Page<Upload> page;

        if (status != null) {
            page = uploadRepository
                    .findByUserIdAndStatusOrderByUploadTimeDesc(
                            userId,
                            status,
                            pageable
                    );
        } else {
            page = uploadRepository
                    .findByUserIdOrderByUploadTimeDesc(userId, pageable);
        }

        return PageResponse.from(page.map(this::mapToResponse));
    }

    @Override
    @Transactional
    public void deleteUpload(String uploadId, Long userId) {

        if (uploadId == null || uploadId.isBlank()) {
            throw new BadRequestException("Invalid uploadId");
        }

        Upload upload = uploadRepository
                .findByUploadIdAndUserId(uploadId, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Upload not found"));

        String filePath = upload.getFilePath();
        uploadRepository.delete(upload);

        if (filePath != null && !filePath.isBlank()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            cleanupStoredFile(filePath, uploadId);
                        }
                    }
            );
        }
    }

    private UploadResponse mapToResponse(Upload upload) {
        return UploadResponse.builder()
                .uploadId(upload.getUploadId())
                .fileName(upload.getFileName())
                .fileSize(upload.getFileSize())
                .fileSizeFormatted(fileSizeFormatterService.format(upload.getFileSize()))
                .status(upload.getStatus().name())
                .uploadTime(upload.getUploadTime())
                .message("Fetched successfully")
                .errorMessage(upload.getProcessingError())
                .totalLogs(safeCount(upload.getTotalLogs()))
                .errorCount(safeCount(upload.getErrorCount()))
                .warnCount(safeCount(upload.getWarnCount()))
                .build();
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private void cleanupStoredFile(String filePath, String uploadId) {

        if (filePath == null || filePath.isBlank()) {
            return;
        }

        try {
            storageService.delete(filePath);
        } catch (Exception cleanupException) {
            log.error(
                    "Stored file cleanup failed uploadId={}",
                    uploadId,
                    cleanupException
            );
        }
    }

    private boolean isInsufficientStorage(Exception exception) {

        String message = exception.getMessage();

        if (message == null) {
            return false;
        }

        String normalized = message.toLowerCase();
        return normalized.contains("no space left")
                || normalized.contains("disk full")
                || normalized.contains("not enough space");
    }
}
