package com.evready.recommender.llm.dto;

public record ModelGenerationResponse(
        String rawOutput,
        String provider,
        String modelName,
        String runConfigJson
) {
}