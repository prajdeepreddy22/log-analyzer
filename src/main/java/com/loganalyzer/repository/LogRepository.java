package com.loganalyzer.repository;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long>,
        JpaSpecificationExecutor<Log> {

    // =====================================================
    // BASIC FETCHERS
    // =====================================================

    Page<Log> findByUploadUploadId(
            String uploadId,
            Pageable pageable
    );

    long countByUploadUploadId(
            String uploadId
    );

    long countByUploadUploadIdAndLevel(
            String uploadId,
            LogLevel level
    );

    // =====================================================
    // AI LOG FETCHERS
    // =====================================================

    Page<Log> findByUploadUploadIdAndLevelIn(
            String uploadId,
            List<LogLevel> levels,
            Pageable pageable
    );

    List<Log> findTop100ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
            String uploadId,
            List<LogLevel> levels
    );

    List<Log> findTop500ByUploadUploadIdOrderByLogTimestampAsc(
            String uploadId
    );

    List<Log> findTop200ByUploadUploadIdAndLevelInOrderByLogTimestampDesc(
            String uploadId,
            List<LogLevel> levels
    );

    List<Log> findTop100ByUploadUploadIdOrderByLogTimestampDesc(
            String uploadId
    );

    List<Log> findTop10ByUploadUploadIdOrderByLogTimestampDesc(
            String uploadId
    );

    Page<Log> findByUploadUploadIdOrderByLogTimestampDesc(
            String uploadId,
            Pageable pageable
    );

    // =====================================================
    // CHAT RELEVANT LOG SEARCH
    // =====================================================

    @Query("""
            SELECT l
            FROM Log l
            WHERE l.upload.uploadId = :uploadId
            AND (
                LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword1, '%'))
                OR LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword2, '%'))
                OR LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword3, '%'))
                OR LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword4, '%'))
                OR LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword5, '%'))
            )
            ORDER BY l.logTimestamp DESC
            """)
    List<Log> searchRelevantLogs(

            @Param("uploadId")
            String uploadId,

            @Param("keyword1")
            String keyword1,

            @Param("keyword2")
            String keyword2,

            @Param("keyword3")
            String keyword3,

            @Param("keyword4")
            String keyword4,

            @Param("keyword5")
            String keyword5
    );
}