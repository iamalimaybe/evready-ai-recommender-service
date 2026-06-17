package com.evready.recommender.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "evready.api")
public record EvreadyApiProperties(
        String baseUrl
) {
}