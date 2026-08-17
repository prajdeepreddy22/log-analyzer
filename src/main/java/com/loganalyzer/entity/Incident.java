package com.loganalyzer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "incidents",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_incidents_upload_user_root",
                columnNames = {"upload_id", "user_id", "root_cause"}
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"upload", "user"})
public class Incident {

    @Id
    @Column(name = "incident_id", length = 36)
    private String incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private Upload upload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_source_id")
    private LogIngestionSource logSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "root_cause", nullable = false, length = 64)
    private String rootCause;

    @Column(name = "root_cause_summary", length = 1000)
    private String rootCauseSummary;

    @Column(name = "severity_score", nullable = false)
    private Byte severityScore;

    @Column(
            name = "confidence_score",
            nullable = false,
            precision = 4,
            scale = 3
    )
    @Builder.Default
    private BigDecimal confidenceScore = new BigDecimal("0.000");

    @Column(name = "occurrence_count", nullable = false)
    private Integer occurrenceCount;

    @Column(name = "first_seen", nullable = false)
    private LocalDateTime firstSeen;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum IncidentStatus {
        OPEN,
        INVESTIGATING,
        FIXED,
        VERIFIED,
        CLOSED
    }
}
