package com.loganalyzer.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "logs")
@Getter
@Setter
@ToString(exclude = {"upload"}) // ✅ Prevent lazy loading issues
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Log {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_id", nullable = false)
    private Upload upload;

    @Column(name = "log_timestamp")
    private LocalDateTime logTimestamp;

    @Column(name = "log_sequence")
    private Long logSequence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private LogLevel level = LogLevel.UNKNOWN;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Column(name = "environment", length = 100)
    private String environment;

    @Column(name = "host_name", length = 200)
    private String hostName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private LogSource source = LogSource.UPLOADED;

    @Column(name = "hash_key", length = 64)
    private String hashKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum LogLevel {
        DEBUG, INFO, WARN, ERROR, FATAL, UNKNOWN
    }

    public enum LogSource {
        UPLOADED, REALTIME
    }
}