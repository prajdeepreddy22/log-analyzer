package com.loganalyzer.service.impl;

import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.dto.response.UploadResponse;
import com.loganalyzer.dto.response.UploadStatusResponse;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.repository.UserRepository;
import com.loganalyzer.service.FileValidationService;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.UploadService;
import com.loganalyzer.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

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
    private final LogIngestionService logIngestionService;

    // ==================== UPLOAD ====================
    @Override
    @Transactional
    public UploadResponse uploadFile(MultipartFile file, String username) {

        fileValidationService.validate(file);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found: " + username));

        String uploadId = UUID.randomUUID().toString();

        log.info("Starting upload: uploadId={}, user={}", uploadId, username);

        String filePath = null;

        try {
            // 1. Store file
            filePath = storageService.store(file, uploadId);

            // 2. Save upload
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

            // 3. Async AFTER COMMIT
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Triggering async ingestion for uploadId={}", uploadId);
                            logIngestionService.process(uploadId);
                        }
                    }
            );

            return UploadResponse.builder()
                    .uploadId(uploadId)
                    .fileName(file.getOriginalFilename())
                    .fileSize(file.getSize())
                    .status(UploadStatus.UPLOADED.name())
                    .uploadTime(upload.getUploadTime())
                    .message("File uploaded successfully. Processing started.")
                    .build();

        } catch (Exception e) {

            log.error("Upload failed: {}", uploadId, e);

            // ✅ CLEANUP FILE
            if (filePath != null) {
                try {
                    storageService.delete(filePath);
                } catch (Exception ex) {
                    log.warn("Failed to cleanup file: {}", filePath);
                }
            }

            throw new BadRequestException("Upload failed: " + e.getMessage());
        }
    }

    // ==================== STATUS ====================
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
                .totalLogs(upload.getTotalLogs())
                .errorCount(upload.getErrorCount())
                .warnCount(upload.getWarnCount())
                .build();
    }

    // ==================== LIST ====================
    @Override
    public PageResponse<UploadResponse> getUserUploads(
            Long userId,
            UploadStatus status,
            Pageable pageable
    ) {

        Page<Upload> page;

        if (status != null) {
            page = uploadRepository
                    .findByUserIdAndStatusOrderByUploadTimeDesc(userId, status, pageable);
        } else {
            page = uploadRepository
                    .findByUserIdOrderByUploadTimeDesc(userId, pageable);
        }

        Page<UploadResponse> responsePage = page.map(this::mapToResponse);

        return PageResponse.from(responsePage);
    }

    // ==================== MAPPER ====================
    private UploadResponse mapToResponse(Upload upload) {
        return UploadResponse.builder()
                .uploadId(upload.getUploadId())
                .fileName(upload.getFileName())
                .fileSize(upload.getFileSize())
                .status(upload.getStatus().name())
                .uploadTime(upload.getUploadTime())
                .message("Fetched successfully")
                .build();
    }
}