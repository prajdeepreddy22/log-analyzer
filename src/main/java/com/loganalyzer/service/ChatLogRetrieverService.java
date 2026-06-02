package com.loganalyzer.service;

import com.loganalyzer.entity.Log;
import com.loganalyzer.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatLogRetrieverService {

    private final LogRepository logRepository;

    public List<Log> fetchRelevantLogs(
            String uploadId,
            String question
    ) {

        log.info(
                "Fetching relevant logs for uploadId={} question={}",
                uploadId,
                question
        );

        // =====================================================
        // EXTRACT KEYWORDS
        // =====================================================
        List<String> keywords =
                extractKeywords(question);

        log.info(
                "Extracted keywords={}",
                keywords
        );

        // =====================================================
        // FETCH MATCHING LOGS
        // =====================================================
        List<Log> logs = logRepository
                .searchRelevantLogs(
                        uploadId,
                        getKeyword(keywords, 0),
                        getKeyword(keywords, 1),
                        getKeyword(keywords, 2),
                        getKeyword(keywords, 3),
                        getKeyword(keywords, 4)
                );

        // =====================================================
        // FALLBACK
        // =====================================================
        if (logs.isEmpty()) {

            log.warn(
                    "No keyword-matched logs found. Using fallback logs"
            );

            logs = logRepository
                    .findTop100ByUploadUploadIdOrderByLogTimestampDesc(
                            uploadId
                    );
        }

        // =====================================================
        // SORT ASCENDING
        // =====================================================
        return logs.stream()
                .sorted(
                        Comparator.comparing(
                                Log::getLogTimestamp,
                                Comparator.nullsLast(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .limit(200)
                .toList();
    }

    // =========================================================
    // BASIC KEYWORD EXTRACTION
    // =========================================================
    private List<String> extractKeywords(
            String question
    ) {

        if (question == null || question.isBlank()) {
            return List.of();
        }

        return Arrays.stream(
                        question
                                .toLowerCase()
                                .replaceAll(
                                        "[^a-zA-Z0-9 ]",
                                        ""
                                )
                                .split("\\s+")
                )
                .filter(word -> word.length() > 2)
                .distinct()
                .limit(10)
                .toList();
    }

    // =========================================================
    // SAFE KEYWORD FETCH
    // =========================================================
    private String getKeyword(
            List<String> keywords,
            int index
    ) {

        if (index >= keywords.size()) {
            return "";
        }

        return keywords.get(index);
    }
}
