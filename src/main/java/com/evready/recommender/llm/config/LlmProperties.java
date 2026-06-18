package com.evready.recommender.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

@ConfigurationProperties(prefix = "llm")
public record LlmProperties(
        String provider,
        Ollama ollama
) {

    public record Ollama(
            String baseUrl,
            String model,
            BigDecimal temperature,
            Integer numPredict,
            Integer numCtx,
            Duration connectTimeout,
            Duration readTimeout
    ) {
    }
}