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

    Page<Log> findByUploadUploadId(String uploadId, Pageable pageable);

    @Query("""
            SELECT l FROM Log l
            WHERE l.upload.uploadId = :uploadId
            AND LOWER(l.message) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Log> searchByKeyword(
            @Param("uploadId") String uploadId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByUploadUploadId(String uploadId);

    long countByUploadUploadIdAndLevel(String uploadId, LogLevel level);

    // ✅ FOR AI (optimized)
    Page<Log> findByUploadUploadIdAndLevelIn(
            String uploadId,
            List<LogLevel> levels,
            Pageable pageable
    );
}