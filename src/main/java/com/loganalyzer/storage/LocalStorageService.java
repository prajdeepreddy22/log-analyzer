package com.loganalyzer.storage;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageProperties storageProperties;

    @Override
    public String store(MultipartFile file, String uploadId) throws IOException {

        LocalDate today = LocalDate.now();

        String dirPath = String.format("%s/%d/%02d/%02d/%s",
                storageProperties.getBasePath(),
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

        String safeFileName = Paths.get(originalFileName).getFileName().toString();
        Path filePath = directory.resolve(safeFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Upload file stored uploadId={}", uploadId);

        return filePath.toString();
    }

    @Override
    public void delete(String filePath) throws IOException {
        Path path = Paths.get(filePath);

        if (Files.exists(path)) {
            Files.delete(path);
            log.info("Stored upload file deleted");
        } else {
            log.warn("Stored upload file was already absent during deletion");
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
