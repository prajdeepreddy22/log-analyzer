package com.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "analysis")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Analysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private Upload upload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "hash_key", nullable = false, length = 64)
    private String hashKey;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "root_cause", columnDefinition = "TEXT")
    private String rootCause;

    @Column(name = "developer_mistake", columnDefinition = "TEXT")
    private String developerMistake;

    @Column(name = "fix_suggestion", columnDefinition = "TEXT")
    private String fixSuggestion;

    @Column(name = "code_fix", columnDefinition = "TEXT")
    private String codeFix;

    @Column(name = "severity_score", columnDefinition = "TINYINT")
    private Integer severityScore;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "prompt_version", nullable = false)
    @Builder.Default
    private Integer promptVersion = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    @Builder.Default
    private AnalysisStatus analysisStatus = AnalysisStatus.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum AnalysisStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, RETRYING
    }
}