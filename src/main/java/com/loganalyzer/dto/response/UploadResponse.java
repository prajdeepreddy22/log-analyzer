package com.loganalyzer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadResponse {

    private String uploadId;
    private String fileName;
    private Long fileSize;
    private String status;
    private LocalDateTime uploadTime;
    private String message;
}