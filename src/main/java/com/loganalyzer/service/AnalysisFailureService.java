package com.loganalyzer.service;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.repository.AnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnalysisFailureService {

    private final AnalysisRepository analysisRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            String uploadId,
            Long userId,
            String errorMessage
    ) {
        analysisRepository.updateStatusAndRetryByUploadId(
                uploadId,
                userId,
                Analysis.AnalysisStatus.FAILED,
                errorMessage
        );
    }
}
