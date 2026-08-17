package com.loganalyzer.repository;

import com.loganalyzer.entity.LogIngestionSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LogIngestionSourceRepository
        extends JpaRepository<LogIngestionSource, Long> {

    List<LogIngestionSource> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<LogIngestionSource> findByIdAndUserId(Long id, Long userId);
}
