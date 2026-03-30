package com.loganalyzer.repository;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long> {

    // Get all logs for an upload ordered by sequence
    Page<Log> findByUploadUploadIdOrderByLogSequenceAsc(
            String uploadId, Pageable pageable);

    // Get logs filtered by level
    Page<Log> findByUploadUploadIdAndLevelOrderByLogSequenceAsc(
            String uploadId, LogLevel level, Pageable pageable);

    // Get ERROR/WARN/FATAL logs for AI analysis
    @Query("SELECT l FROM Log l WHERE l.upload.uploadId = :uploadId " +
            "AND l.level IN ('ERROR', 'WARN', 'FATAL') " +
            "ORDER BY l.level DESC, l.logTimestamp DESC")
    List<Log> findCriticalLogsForAnalysis(@Param("uploadId") String uploadId,
                                          Pageable pageable);

    // Count logs by level for an upload
    long countByUploadUploadIdAndLevel(String uploadId, LogLevel level);

    // Count total logs for an upload
    long countByUploadUploadId(String uploadId);

    // Get logs by hash key (for caching)
    List<Log> findByHashKey(String hashKey);

    // Delete all logs for an upload
    void deleteByUploadUploadId(String uploadId);
}