package com.loganalyzer.repository;

import com.loganalyzer.entity.Incident;
import com.loganalyzer.entity.Incident.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, String> {

    Optional<Incident> findByUploadUploadIdAndUserIdAndRootCause(
            String uploadId,
            Long userId,
            String rootCause
    );

    List<Incident> findByUserIdOrderBySeverityScoreDescOccurrenceCountDescLastSeenDesc(
            Long userId
    );

    Page<Incident> findByUserId(Long userId, Pageable pageable);

    Page<Incident> findByUserIdAndStatus(
            Long userId,
            IncidentStatus status,
            Pageable pageable
    );

    Optional<Incident> findByIncidentIdAndUserId(
            String incidentId,
            Long userId
    );
}
