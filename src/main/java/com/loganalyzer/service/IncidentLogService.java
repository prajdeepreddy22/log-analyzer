package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class IncidentLogService {

    public List<Log> buildIncidentContext(List<Log> logs) {

        if (logs == null || logs.isEmpty()) {
            return List.of();
        }

        List<Log> sorted = logs.stream()
                .sorted(timestampComparator())
                .toList();

        List<Log> result = new ArrayList<>();

        for (int i = 0; i < sorted.size(); i++) {

            Log current = sorted.get(i);

            // Always include ERROR/WARN as anchor
            if (isCritical(current)) {

                int start = Math.max(0, i - 2);
                int end = Math.min(sorted.size(), i + 3);

                for (int j = start; j < end; j++) {
                    result.add(sorted.get(j));
                }
            }
        }

        return result.stream().distinct().toList();
    }

    private boolean isCritical(Log log) {
        return log.getLevel() != null
                && (log.getLevel().name().equals("ERROR")
                || log.getLevel().name().equals("WARN")
                || log.getLevel().name().equals("FATAL"));
    }

    private Comparator<Log> timestampComparator() {
        return Comparator.comparing(
                Log::getLogTimestamp,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
    }
}
