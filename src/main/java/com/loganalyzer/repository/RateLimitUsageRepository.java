package com.loganalyzer.repository;

import com.loganalyzer.entity.RateLimitUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RateLimitUsageRepository
        extends JpaRepository<RateLimitUsage, Long> {

    Optional<RateLimitUsage> findByUserIdAndUsageDate(
            Long userId,
            LocalDate usageDate);
}
