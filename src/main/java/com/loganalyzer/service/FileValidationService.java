package com.loganalyzer.service;

import com.loganalyzer.exception.FileValidationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileValidationService {

    private static final long MAX_SIZE = 10 * 1024 * 1024;

    public void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new FileValidationException("File cannot be empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new FileValidationException("File exceeds 10MB limit");
        }

        String name = file.getOriginalFilename();

        if (name == null ||
                !(name.endsWith(".log") || name.endsWith(".txt"))) {
            throw new FileValidationException("Only .log and .txt allowed");
        }
    }
}