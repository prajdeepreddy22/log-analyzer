package com.loganalyzer.specification;

import com.loganalyzer.dto.request.LogFilterRequest;
import com.loganalyzer.entity.Log;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class LogSpecification {

    public static Specification<Log> build(String uploadId, LogFilterRequest req) {
        return Specification.where(hasUploadId(uploadId))
                .and(hasLevel(req.getLevel()))
                .and(hasService(req.getServiceName()))
                .and(hasKeyword(req.getKeyword()))
                .and(betweenDates(req.getStartDate(), req.getEndDate()));
    }

    public static Specification<Log> hasUploadId(String uploadId) {
        return (r, q, cb) ->
                cb.equal(r.get("upload").get("uploadId"), uploadId);
    }

    public static Specification<Log> hasLevel(Log.LogLevel level) {
        return (r, q, cb) ->
                level == null ? null : cb.equal(r.get("level"), level);
    }

    public static Specification<Log> hasService(String service) {
        return (r, q, cb) ->
                (service == null || service.isBlank())
                        ? null
                        : cb.like(cb.lower(r.get("serviceName")),
                        "%" + service.toLowerCase() + "%");
    }

    public static Specification<Log> hasKeyword(String keyword) {
        return (r, q, cb) ->
                (keyword == null || keyword.isBlank())
                        ? null
                        : cb.like(cb.lower(r.get("message")),
                        "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Log> betweenDates(LocalDateTime start, LocalDateTime end) {
        return (r, q, cb) -> {
            if (start != null && end != null)
                return cb.between(r.get("logTimestamp"), start, end);
            if (start != null)
                return cb.greaterThanOrEqualTo(r.get("logTimestamp"), start);
            if (end != null)
                return cb.lessThanOrEqualTo(r.get("logTimestamp"), end);
            return null;
        };
    }
}