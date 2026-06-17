package com.evready.recommender.llm.dto;

import java.math.BigDecimal;

public record ModelGenerationResponse(
        String rawOutput,
        String provider,
        String modelName,
        BigDecimal temperature,
        String runConfigJson
) {
}