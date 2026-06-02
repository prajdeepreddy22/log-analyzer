package com.loganalyzer.repository;

import com.loganalyzer.entity.Analysis;
import com.loganalyzer.entity.Analysis.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    // =========================
    // PRIMARY LOOKUP — by upload + user
    // Used everywhere: upsert, getAnalysis, getStatus
    // =========================

    Optional<Analysis> findByUploadUploadIdAndUserId(
            String uploadId, Long userId);

    // =========================
    // CACHE DECISION — find any analysis with this hash for this user
    // Used by CacheDecisionService to decide SKIP/RETRY/NEW/IN_PROGRESS
    // =========================

    Optional<Analysis> findFirstByHashKeyAndUserIdOrderByUpdatedAtDesc(
            String hashKey, Long userId);

    // =========================
    // CACHE LOOKUP — find completed analysis with same hash
    // Used for cross-upload cache reuse (SKIP decision)
    // findFirst because multiple uploads can share same hash
    // =========================

    Optional<Analysis> findFirstByHashKeyAndUserIdAndAnalysisStatusOrderByUpdatedAtDesc(
            String hashKey, Long userId, AnalysisStatus status);

    // =========================
    // STATUS FETCH — lightweight, returns only status field
    // =========================

    @Query("SELECT a.analysisStatus FROM Analysis a " +
            "WHERE a.upload.uploadId = :uploadId AND a.user.id = :userId")
    Optional<AnalysisStatus> findStatusByUploadIdAndUserId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId);

    // =========================
    // STATUS UPDATE — by uploadId (correct, matches new schema)
    // Used by AIProcessingService after AI call completes/fails
    // =========================

    @Modifying
    @Transactional
    @Query("UPDATE Analysis a SET a.analysisStatus = :status, " +
            "a.retryCount = a.retryCount + 1, " +
            "a.errorMessage = :errorMessage " +
            "WHERE a.upload.uploadId = :uploadId " +
            "AND a.user.id = :userId")
    int updateStatusAndRetryByUploadId(
            @Param("uploadId") String uploadId,
            @Param("userId") Long userId,
            @Param("status") AnalysisStatus status,
            @Param("errorMessage") String errorMessage);

    // =========================
    // STATS — used by observability / metrics
    // =========================

    long countByAnalysisStatus(AnalysisStatus status);

    // =========================
    // HISTORY — user's analysis list, newest first
    // =========================

    List<Analysis> findByUserIdOrderByCreatedAtDesc(Long userId);
}
