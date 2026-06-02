package com.loganalyzer.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    @Value("${app.ai.core-pool-size:4}")
    private int corePoolSize;

    @Value("${app.ai.max-pool-size:8}")
    private int maxPoolSize;

    @Value("${app.ai.queue-capacity:200}")
    private int queueCapacity;

    @Value("${app.ai.keep-alive-seconds:60}")
    private int keepAliveSeconds;

    @Bean(name = "aiExecutor")
    public TaskExecutor aiExecutor() {

        log.info("Initializing AI async executor");

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);

        executor.setThreadNamePrefix("AI-WORKER-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.CallerRunsPolicy()
        );

        executor.initialize();

        log.info("AI async executor initialized successfully");

        // propagates Spring Security context into async threads
        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
