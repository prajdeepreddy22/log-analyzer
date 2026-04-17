package com.loganalyzer.service;

import com.loganalyzer.dto.response.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadResponse uploadFile(MultipartFile file, String username);
}