package com.loganalyzer.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Ai ai = new Ai();
    private Upload upload = new Upload();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private long expiration;
    }

    @Getter
    @Setter
    public static class Ai {
        private int maxLogs;
        private int maxChars;
    }

    @Getter
    @Setter
    public static class Upload {
        private String maxFileSize;
        private String allowedTypes;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int maxRequests;
        private int windowMinutes;
    }
}