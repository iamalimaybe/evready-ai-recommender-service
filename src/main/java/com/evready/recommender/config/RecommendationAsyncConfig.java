package com.evready.recommender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class RecommendationAsyncConfig {

    @Bean(name = "recommendationTaskExecutor")
    public ThreadPoolTaskExecutor recommendationTaskExecutor(
            @Value("${recommendation.processing.core-pool-size:1}") int corePoolSize,
            @Value("${recommendation.processing.max-pool-size:1}") int maxPoolSize,
            @Value("${recommendation.processing.queue-capacity:10}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("recommendation-worker-");
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.initialize();
        return executor;
    }
}