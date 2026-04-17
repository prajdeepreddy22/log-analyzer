package com.loganalyzer.repository;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Analysis.AnalysisStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Optional<Analysis> findByUploadUploadIdAndUserId(
            String uploadId, Long userId);

    Optional<Analysis> findByHashKeyAndUserIdAndAnalysisStatus(
            String hashKey, Long userId, AnalysisStatus status);

    Optional<Analysis> findByHashKeyAndUserId(String hashKey, Long userId);

    @Query("SELECT a.analysisStatus FROM Analysis a " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    Optional<AnalysisStatus> findStatusByUploadIdAndUserId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId);

    // ✅ FIXED
    @Modifying
    @Transactional
    @Query("UPDATE Analysis a SET a.analysisStatus = :status " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    int updateStatus(@Param("uploadId") String uploadId,
                     @Param("userId") Long userId,
                     @Param("status") AnalysisStatus status);

    // ✅ FIXED
    @Modifying
    @Transactional
    @Query("UPDATE Analysis a SET a.analysisStatus = :status, " +
            "a.retryCount = a.retryCount + 1, a.errorMessage = :errorMessage " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    int updateStatusAndRetry(@Param("uploadId") String uploadId,
                             @Param("userId") Long userId,
                             @Param("status") AnalysisStatus status,
                             @Param("errorMessage") String errorMessage);

    long countByAnalysisStatus(AnalysisStatus status);
}