package com.loganalyzer.repository;

import com.loganalyzer.entity.Incident;
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
}
