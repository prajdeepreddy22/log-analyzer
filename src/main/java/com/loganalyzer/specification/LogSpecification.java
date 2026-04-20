package com.loganalyzer.specification;

import com.loganalyzer.entity.Log;
import com.loganalyzer.entity.Log.LogLevel;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class LogSpecification {

    public static Specification<Log> hasUploadId(String uploadId) {
        return (root, query, cb) ->
                uploadId == null || uploadId.isBlank()
                        ? null
                        : cb.equal(root.get("upload").get("uploadId"), uploadId);
    }

    public static Specification<Log> hasLevel(LogLevel level) {
        return (root, query, cb) ->
                level == null ? null : cb.equal(root.get("level"), level);
    }

    public static Specification<Log> hasServiceName(String serviceName) {
        return (root, query, cb) ->
                serviceName == null || serviceName.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("serviceName")),
                        "%" + serviceName.toLowerCase() + "%");
    }

    public static Specification<Log> containsKeyword(String keyword) {
        return (root, query, cb) ->
                keyword == null || keyword.isBlank()
                        ? null
                        : cb.like(cb.lower(root.get("message")),
                        "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Log> betweenDates(LocalDateTime start, LocalDateTime end) {
        return (root, query, cb) -> {
            if (start != null && end != null) {
                return cb.between(root.get("logTimestamp"), start, end);
            } else if (start != null) {
                return cb.greaterThanOrEqualTo(root.get("logTimestamp"), start);
            } else if (end != null) {
                return cb.lessThanOrEqualTo(root.get("logTimestamp"), end);
            }
            return null;
        };
    }
}