package com.loganalyzer;

import com.loganalyzer.config.AIProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AIProperties.class)
public class LogAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogAnalyzerApplication.class, args);
    }
}