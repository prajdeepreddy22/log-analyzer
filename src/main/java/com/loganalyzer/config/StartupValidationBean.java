package com.loganalyzer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupValidationBean
        implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment env;
    private final RedisConnectionFactory redisConnectionFactory;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validateRedis();
        validateStoragePath();
    }

    private void validateRedis() {
        boolean redisRequired = env.getProperty(
                "aeip.redis.required",
                Boolean.class,
                true
        );

        if (!redisRequired) {
            log.warn(
                    "Redis startup validation is disabled. "
                            + "This should only be used for tests or local troubleshooting."
            );
            return;
        }

        try (RedisConnection connection =
                     redisConnectionFactory.getConnection()) {
            String response = connection.ping();

            if (!"PONG".equalsIgnoreCase(response)) {
                throw new IllegalStateException(
                        "Redis health check failed: " + response
                );
            }
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Redis is required for ingestion deduplication and AI analysis cache. "
                            + "Start Redis or configure REDIS_HOST/REDIS_PORT correctly.",
                    exception
            );
        }
    }

    private void validateStoragePath() {
        String storagePath = env.getProperty("storage.base.path", "");

        if (storagePath.isBlank()
                || storagePath.contains("/app/uploads")
                || storagePath.contains("uploads")) {
            log.warn(
                    "STORAGE_BASE_PATH is set to a local path ({}). "
                            + "This is not suitable for production on disposable or scaled containers. "
                            + "Migrate to S3 or EFS before production use.",
                    storagePath
            );
        }
    }
}
