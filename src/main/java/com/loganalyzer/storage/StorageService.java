package com.loganalyzer.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    String store(MultipartFile file, String uploadId) throws IOException;

    void delete(String filePath) throws IOException;

    InputStream read(String filePath) throws IOException;
}