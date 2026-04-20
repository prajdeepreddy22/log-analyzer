package com.loganalyzer.repository;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // ✅ ADD THIS
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long>,
        JpaSpecificationExecutor<Log> {   // ✅ ADD THIS

    Page<Log> findByUploadUploadIdOrderByLogSequenceAsc(
            String uploadId, Pageable pageable);

    Page<Log> findByUploadUploadIdAndLevelOrderByLogSequenceAsc(
            String uploadId, LogLevel level, Pageable pageable);

    @Query("SELECT l FROM Log l WHERE l.upload.uploadId = :uploadId " +
            "AND l.level IN ('ERROR', 'WARN', 'FATAL') " +
            "ORDER BY l.level DESC, l.logTimestamp DESC")
    List<Log> findCriticalLogsForAnalysis(@Param("uploadId") String uploadId,
                                          Pageable pageable);

    long countByUploadUploadIdAndLevel(String uploadId, LogLevel level);

    long countByUploadUploadId(String uploadId);

    List<Log> findByHashKey(String hashKey);

    void deleteByUploadUploadId(String uploadId);
}