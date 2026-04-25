package com.loganalyzer.service;

import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.dto.response.UploadResponse;
import com.loganalyzer.dto.response.UploadStatusResponse;
import com.loganalyzer.entity.UploadStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadResponse uploadFile(MultipartFile file, String username);

    UploadStatusResponse getUploadStatus(String uploadId, Long userId);

    PageResponse<UploadResponse> getUserUploads(
            Long userId,
            UploadStatus status,
            Pageable pageable
    );
}