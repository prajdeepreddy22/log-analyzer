package com.loganalyzer.repository;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Analysis.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // Find completed analysis by upload
    Optional<Analysis> findByUploadUploadIdAndUserId(
            String uploadId, Long userId);

    // Cache lookup — check if completed analysis exists for this hash + user
    Optional<Analysis> findByHashKeyAndUserIdAndAnalysisStatus(
            String hashKey, Long userId, AnalysisStatus status);

    // Check if any analysis exists for this hash + user
    Optional<Analysis> findByHashKeyAndUserId(String hashKey, Long userId);

    // Get analysis status only
    @Query("SELECT a.analysisStatus FROM Analysis a " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    Optional<AnalysisStatus> findStatusByUploadIdAndUserId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId);

    // Update analysis status
    @Modifying
    @Query("UPDATE Analysis a SET a.analysisStatus = :status " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    int updateStatus(@Param("uploadId") String uploadId,
                     @Param("userId") Long userId,
                     @Param("status") AnalysisStatus status);

    // Update retry count and status on failure
    @Modifying
    @Query("UPDATE Analysis a SET a.analysisStatus = :status, " +
            "a.retryCount = a.retryCount + 1, a.errorMessage = :errorMessage " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    int updateStatusAndRetry(@Param("uploadId") String uploadId,
                             @Param("userId") Long userId,
                             @Param("status") AnalysisStatus status,
                             @Param("errorMessage") String errorMessage);

    // Count analyses by status (for observability)
    long countByAnalysisStatus(AnalysisStatus status);
}