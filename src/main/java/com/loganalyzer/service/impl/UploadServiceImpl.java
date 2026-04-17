package com.loganalyzer.service.impl;

import com.loganalyzer.dto.response.UploadResponse;
import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.UploadRepository;
import com.loganalyzer.repository.UserRepository;
import com.loganalyzer.service.FileValidationService;
import com.loganalyzer.service.LogIngestionService;
import com.loganalyzer.service.UploadService;
import com.loganalyzer.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    @Transactional
    public UploadResponse uploadFile(MultipartFile file, String username) {

        fileValidationService.validate(file);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        String uploadId = UUID.randomUUID().toString();

        log.info("Starting upload: uploadId={}, user={}", uploadId, username);

        try {
            // 1. Store file
            String filePath = storageService.store(file, uploadId);

            // 2. Save upload record
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

            // ✅ CRITICAL FIX: Execute async AFTER DB commit
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Triggering async log ingestion AFTER COMMIT for uploadId={}", uploadId);
                            logIngestionService.process(uploadId);
                        }
                    }
            );

            // 3. Response
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
            throw new RuntimeException("Upload failed: " + e.getMessage());
        }
    }
}