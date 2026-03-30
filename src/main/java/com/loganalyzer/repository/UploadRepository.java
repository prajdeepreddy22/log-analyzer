package com.loganalyzer.repository;

import com.loganalyzer.entity.Upload;
import com.loganalyzer.entity.Upload.UploadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UploadRepository extends JpaRepository<Upload, String> {

    // Get all uploads for a specific user with pagination
    Page<Upload> findByUserIdOrderByUploadTimeDesc(Long userId, Pageable pageable);

    // Get all uploads for a specific user filtered by status
    Page<Upload> findByUserIdAndStatusOrderByUploadTimeDesc(
            Long userId, UploadStatus status, Pageable pageable);

    // Find specific upload belonging to a user (multi-tenancy check)
    Optional<Upload> findByUploadIdAndUserId(String uploadId, Long userId);

    // Count uploads by status for a user
    long countByUserIdAndStatus(Long userId, UploadStatus status);

    // Update status of an upload
    @Modifying
    @Query("UPDATE Upload u SET u.status = :status WHERE u.uploadId = :uploadId")
    int updateStatus(@Param("uploadId") String uploadId,
                     @Param("status") UploadStatus status);

    // Update log counts after parsing
    @Modifying
    @Query("UPDATE Upload u SET u.totalLogs = :totalLogs, " +
            "u.errorCount = :errorCount, u.warnCount = :warnCount " +
            "WHERE u.uploadId = :uploadId")
    int updateLogCounts(@Param("uploadId") String uploadId,
                        @Param("totalLogs") int totalLogs,
                        @Param("errorCount") int errorCount,
                        @Param("warnCount") int warnCount);
}