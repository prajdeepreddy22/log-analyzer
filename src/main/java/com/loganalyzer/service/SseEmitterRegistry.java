package com.loganalyzer.service;

import com.loganalyzer.dto.response.RealtimeEventResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseEmitterRegistry {

    private static final long TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters =
            new ConcurrentHashMap<>();

    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);

        emitters.computeIfAbsent(userId, ignored -> new CopyOnWriteArrayList<>())
                .add(emitter);

        log.debug("Registered SSE emitter userId={} activeEmitters={}",
                userId,
                activeCount(userId));

        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(error -> cleanup.run());

        send(emitter, "CONNECTED",
                RealtimeEventResponse.of(
                        "CONNECTED",
                        Map.of("message", "Realtime stream connected")
                ));

        return emitter;
    }

    public void sendToUser(
            Long userId,
            String eventName,
            Object payload
    ) {
        List<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters == null || userEmitters.isEmpty()) {
            log.debug("No SSE emitters registered userId={} event={}",
                    userId,
                    eventName);
            return;
        }

        log.debug("Sending SSE event userId={} event={} emitters={}",
                userId,
                eventName,
                userEmitters.size());

        RealtimeEventResponse event =
                RealtimeEventResponse.of(eventName, payload);

        for (SseEmitter emitter : userEmitters) {
            if (!send(emitter, eventName, event)) {
                remove(userId, emitter);
            }
        }
    }

    int activeCount(Long userId) {
        return emitters.getOrDefault(userId, new CopyOnWriteArrayList<>())
                .size();
    }

    private boolean send(
            SseEmitter emitter,
            String eventName,
            RealtimeEventResponse payload
    ) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(eventName)
                            .data(payload)
            );
            return true;
        } catch (IOException | IllegalStateException ex) {
            log.debug("Removing stale SSE emitter type={}",
                    ex.getClass().getSimpleName());
            return false;
        }
    }

    void remove(
            Long userId,
            SseEmitter emitter
    ) {
        List<SseEmitter> userEmitters = emitters.get(userId);

        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);

        if (userEmitters.isEmpty()) {
            emitters.remove(userId);
        }

        log.debug("Removed SSE emitter userId={} activeEmitters={}",
                userId,
                activeCount(userId));
    }
}
