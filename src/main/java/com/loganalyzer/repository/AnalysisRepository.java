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

    // =========================
    // FETCH METHODS
    // =========================

    Optional<Analysis> findByUploadUploadIdAndUserId(
            String uploadId, Long userId);

    Optional<Analysis> findByHashKeyAndUserId(
            String hashKey, Long userId);

    Optional<Analysis> findByHashKeyAndUserIdAndAnalysisStatus(
            String hashKey, Long userId, AnalysisStatus status);

    // =========================
    // STATUS FETCH
    // =========================

    @Query("SELECT a.analysisStatus FROM Analysis a " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    Optional<AnalysisStatus> findStatusByUploadIdAndUserId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId);

    // =========================
    // HASH-BASED UPDATES (FINAL APPROACH)
    // =========================

    @Modifying
    @Transactional
    @Query("UPDATE Analysis a SET a.analysisStatus = :status " +
            "WHERE a.hashKey = :hash AND a.user.id = :userId")
    int updateStatusByHash(@Param("hash") String hash,
                           @Param("userId") Long userId,
                           @Param("status") AnalysisStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Analysis a SET a.analysisStatus = :status, " +
            "a.retryCount = a.retryCount + 1, a.errorMessage = :errorMessage " +
            "WHERE a.hashKey = :hash AND a.user.id = :userId")
    int updateStatusAndRetryByHash(@Param("hash") String hash,
                                   @Param("userId") Long userId,
                                   @Param("status") AnalysisStatus status,
                                   @Param("errorMessage") String errorMessage);

    // =========================
    // STATS
    // =========================

    long countByAnalysisStatus(AnalysisStatus status);
}