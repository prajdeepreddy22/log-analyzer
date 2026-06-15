package com.loganalyzer.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StartupValidationBean
        implements ApplicationListener<ApplicationReadyEvent> {

    private final Environment env;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
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
