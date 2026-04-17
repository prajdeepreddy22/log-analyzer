package com.loganalyzer.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Validated
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Ai ai = new Ai();
    private Upload upload = new Upload();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {
        @NotBlank
        private String secret;

        @Min(1000)
        private long expiration;
    }

    @Getter
    @Setter
    public static class Ai {
        @Min(1)
        private int maxLogs;

        @Min(100)
        private int maxChars;
    }

    @Getter
    @Setter
    public static class Upload {
        @NotBlank
        private String maxFileSize;

        @NotBlank
        private String allowedTypes;
    }

    @Getter
    @Setter
    public static class RateLimit {
        @Min(1)
        private int maxRequests;

        @Min(1)
        private int windowMinutes;
    }
}