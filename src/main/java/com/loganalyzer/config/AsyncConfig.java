package com.loganalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Value("${app.ai.async.core-pool-size:4}")
    private int corePoolSize;

    @Value("${app.ai.async.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${app.ai.async.queue-capacity:200}")
    private int queueCapacity;

    @Value("${app.ai.async.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean(name = "aiExecutor")
    public ThreadPoolTaskExecutor aiExecutor() {

        log.info("Initializing AI async executor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);

        executor.setThreadNamePrefix("AI-WORKER-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        log.info("AI async executor initialized successfully");

        return executor;
    }
}