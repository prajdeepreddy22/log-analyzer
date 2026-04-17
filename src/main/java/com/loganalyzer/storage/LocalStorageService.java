package com.loganalyzer.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private static final String BASE_DIR = "uploads";

    @Override
    public String store(MultipartFile file, String uploadId) throws IOException {

        LocalDate today = LocalDate.now();

        String dirPath = String.format("%s/%d/%02d/%02d/%s",
                BASE_DIR,
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                uploadId
        );

        Path directory = Paths.get(dirPath);

        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null) {
            throw new StorageException("File name is missing");
        }

        Path filePath = directory.resolve(originalFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File stored at: {}", filePath.toAbsolutePath());

        return filePath.toString();
    }

    @Override
    public void delete(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (Files.exists(path)) {
            Files.delete(path);
            log.info("File deleted: {}", filePath);
        } else {
            log.warn("File not found for deletion: {}", filePath);
        }
    }

    @Override
    public InputStream read(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (!Files.exists(path)) {
            throw new StorageException("File not found: " + filePath);
        }

        return Files.newInputStream(path, StandardOpenOption.READ);
    }
}