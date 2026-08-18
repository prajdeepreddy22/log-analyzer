package com.loganalyzer.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterRegistryTest {

    @Test
    void registersAndRemovesEmitterForUser() {

        SseEmitterRegistry registry = new SseEmitterRegistry();

        SseEmitter emitter = registry.register(1L);

        assertThat(registry.activeCount(1L)).isEqualTo(1);

        registry.remove(1L, emitter);

        assertThat(registry.activeCount(1L)).isZero();
    }

    @Test
    void sendsEventToRegisteredUserWithoutClosingEmitter() {

        SseEmitterRegistry registry = new SseEmitterRegistry();

        registry.register(1L);

        registry.sendToUser(
                1L,
                "LOG_INGESTED",
                Map.of("count", 1)
        );

        assertThat(registry.activeCount(1L)).isEqualTo(1);
    }
}
