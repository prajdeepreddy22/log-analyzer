package com.loganalyzer.controller;

import com.loganalyzer.dto.response.PageResponse;
import com.loganalyzer.dto.response.UploadResponse;
import com.loganalyzer.dto.response.UploadStatusResponse;
import com.loganalyzer.entity.UploadStatus;
import com.loganalyzer.exception.BadRequestException;
import com.loganalyzer.exception.UnauthorizedException;
import com.loganalyzer.service.UploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/uploads")
@RequiredArgsConstructor
@Slf4j
public class UploadController {

    private final UploadService uploadService;

    // ==================== HELPER ====================
    private Long extractUserId(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            throw new UnauthorizedException("Unauthorized");
        }

        return userId;
    }

    // ==================== UPLOAD ====================
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {

        if (authentication == null || authentication.getName() == null) {
            throw new UnauthorizedException("User not authenticated");
        }

        String username = authentication.getName();

        log.info("Upload request from user={}", username);

        UploadResponse response = uploadService.uploadFile(file, username);

        return ResponseEntity.accepted().body(response);
    }

    // ==================== STATUS ====================
    @GetMapping("/{uploadId}")
    public ResponseEntity<UploadStatusResponse> getUploadStatus(
            @PathVariable String uploadId,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        UploadStatusResponse response =
                uploadService.getUploadStatus(uploadId, userId);

        return ResponseEntity.ok(response);
    }

    // ==================== LIST ====================
    @GetMapping
    public ResponseEntity<PageResponse<UploadResponse>> getUploads(
            @RequestParam(required = false) UploadStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request
    ) {

        Long userId = extractUserId(request);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(safePage, safeSize);

        PageResponse<UploadResponse> response =
                uploadService.getUserUploads(userId, status, pageable);

        return ResponseEntity.ok(response);
    }
}