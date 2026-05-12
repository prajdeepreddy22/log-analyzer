package com.loganalyzer.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchAnalysisJob {

    private String batchId;

    private Long userId;

    private List<String> uploadIds;

    private int totalUploads;

    private int processedUploads;

    private String status;

    private LocalDateTime createdAt;
}