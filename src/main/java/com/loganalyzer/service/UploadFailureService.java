package com.loganalyzer.service;

import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.repository.UploadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadFailureService {

    private final UploadRepository uploadRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String uploadId, String errorMessage) {

        uploadRepository.findById(uploadId).ifPresent(upload -> {
            upload.setStatus(UploadStatus.FAILED);
            upload.setProcessingError(errorMessage);
            uploadRepository.save(upload);
        });
    }
}
