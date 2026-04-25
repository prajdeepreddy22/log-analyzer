package com.loganalyzer.repository;

import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {

    Page<Upload> findByUserIdOrderByUploadTimeDesc(Long userId, Pageable pageable);

    Page<Upload> findByUserIdAndStatusOrderByUploadTimeDesc(
            Long userId, UploadStatus status, Pageable pageable);

    // ✅ ALWAYS USE THIS
    Optional<Upload> findByUploadIdAndUserId(String uploadId, Long userId);

    long countByUserIdAndStatus(Long userId, UploadStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Upload u SET u.status = :status WHERE u.uploadId = :uploadId")
    int updateStatus(String uploadId, UploadStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Upload u SET u.totalLogs = :totalLogs, " +
            "u.errorCount = :errorCount, u.warnCount = :warnCount " +
            "WHERE u.uploadId = :uploadId")
    int updateLogCounts(String uploadId, int totalLogs, int errorCount, int warnCount);
}