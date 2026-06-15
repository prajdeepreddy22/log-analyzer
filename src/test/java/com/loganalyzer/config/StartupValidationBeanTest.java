package com.loganalyzer.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupValidationBeanTest {

    @Test
    void warnsWhenStorageUsesLocalContainerPath(CapturedOutput output) {

        MockEnvironment environment = new MockEnvironment()
                .withProperty("storage.base.path", "/app/uploads");

        new StartupValidationBean(environment).onApplicationEvent(null);

        assertThat(output)
                .contains("STORAGE_BASE_PATH is set to a local path")
                .contains("Migrate to S3 or EFS before production use");
    }

    @Test
    void doesNotWarnForNonLocalStoragePath(CapturedOutput output) {

        MockEnvironment environment = new MockEnvironment()
                .withProperty("storage.base.path", "s3://logai-production");

        new StartupValidationBean(environment).onApplicationEvent(null);

        assertThat(output)
                .doesNotContain("STORAGE_BASE_PATH is set to a local path");
    }
}
