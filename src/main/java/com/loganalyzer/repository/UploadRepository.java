package com.loganalyzer.repository;

import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.UploadStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {

    Page<Upload> findByUserIdOrderByUploadTimeDesc(Long userId, Pageable pageable);

    Page<Upload> findByUserIdAndStatusOrderByUploadTimeDesc(
            Long userId, UploadStatus status, Pageable pageable);

    Optional<Upload> findByUploadIdAndUserId(String uploadId, Long userId);

    // ✅ ADD THIS (CRITICAL FIX)
    Optional<Upload> findByUploadId(String uploadId);

    long countByUserIdAndStatus(Long userId, UploadStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Upload u SET u.status = :status WHERE u.uploadId = :uploadId")
    int updateStatus(@Param("uploadId") String uploadId,
                     @Param("status") UploadStatus status);

    @Modifying
    @Transactional
    @Query("UPDATE Upload u SET u.totalLogs = :totalLogs, " +
            "u.errorCount = :errorCount, u.warnCount = :warnCount " +
            "WHERE u.uploadId = :uploadId")
    int updateLogCounts(@Param("uploadId") String uploadId,
                        @Param("totalLogs") int totalLogs,
                        @Param("errorCount") int errorCount,
                        @Param("warnCount") int warnCount);
}