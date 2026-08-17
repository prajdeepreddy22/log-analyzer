package com.loganalyzer.service;

import com.loganalyzer.dto.request.CreateLogSourceRequest;
import com.loganalyzer.dto.request.UpdateLogSourceStatusRequest;
import com.loganalyzer.dto.response.LogSourceResponse;
import com.loganalyzer.entity.LogIngestionSource;
import com.loganalyzer.entity.User;
import com.loganalyzer.exception.ResourceNotFoundException;
import com.loganalyzer.repository.LogIngestionSourceRepository;
import com.loganalyzer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LogSourceService {

    private final LogIngestionSourceRepository sourceRepository;
    private final UserRepository userRepository;

    @Transactional
    public LogSourceResponse createSource(
            Long userId,
            CreateLogSourceRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        LogIngestionSource source = sourceRepository.save(
                LogIngestionSource.builder()
                        .user(user)
                        .sourceName(request.getSourceName().trim())
                        .sourceType(request.getSourceType())
                        .status(LogIngestionSource.SourceStatus.ACTIVE)
                        .build()
        );

        return toResponse(source);
    }

    @Transactional(readOnly = true)
    public List<LogSourceResponse> getSources(Long userId) {

        return sourceRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LogSourceResponse updateStatus(
            Long userId,
            Long sourceId,
            UpdateLogSourceStatusRequest request
    ) {

        LogIngestionSource source = sourceRepository
                .findByIdAndUserId(sourceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Log source not found"));

        source.setStatus(request.getStatus());

        return toResponse(sourceRepository.save(source));
    }

    public LogSourceResponse toResponse(LogIngestionSource source) {

        return LogSourceResponse.builder()
                .id(source.getId())
                .sourceName(source.getSourceName())
                .sourceType(source.getSourceType().name())
                .status(source.getStatus().name())
                .internalUploadId(
                        source.getInternalUpload() != null
                                ? source.getInternalUpload().getUploadId()
                                : null
                )
                .lastIngestedAt(source.getLastIngestedAt())
                .build();
    }
}
