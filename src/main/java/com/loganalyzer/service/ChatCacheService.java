package com.loganalyzer.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatCacheService {

    // =========================================================
    // CACHE STORAGE
    // =========================================================
    private final Map<String, CachedChatResponse> cache =
            new ConcurrentHashMap<>();

    // =========================================================
    // GET
    // =========================================================
    public CachedChatResponse get(String key) {

        CachedChatResponse cached = cache.get(key);

        if (cached == null) {
            return null;
        }

        // OPTIONAL TTL CHECK
        if (cached.getCreatedAt()
                .isBefore(LocalDateTime.now().minusMinutes(30))) {

            cache.remove(key);

            return null;
        }

        return cached;
    }

    // =========================================================
    // PUT
    // =========================================================
    public void put(String key, String answer) {

        cache.put(
                key,
                new CachedChatResponse(
                        answer,
                        LocalDateTime.now()
                )
        );
    }

    // =========================================================
    // CACHE MODEL
    // =========================================================
    @Getter
    @RequiredArgsConstructor
    public static class CachedChatResponse {

        private final String answer;

        private final LocalDateTime createdAt;
    }
}