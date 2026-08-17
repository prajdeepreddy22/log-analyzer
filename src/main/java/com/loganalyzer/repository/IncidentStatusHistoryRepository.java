package com.loganalyzer.repository;

import com.loganalyzer.entity.IncidentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentStatusHistoryRepository
        extends JpaRepository<IncidentStatusHistory, Long> {

    List<IncidentStatusHistory> findByIncidentIncidentIdAndIncidentUserIdOrderByChangedAtAsc(
            String incidentId,
            Long userId
    );
}
