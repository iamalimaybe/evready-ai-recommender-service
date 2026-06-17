package com.evready.recommender.recommendation.service.dto;

import java.util.List;

public record RecommendationExecutionRecommendation(
        Long vehicleId,
        Integer rank,
        String matchReason,
        List<String> tradeoffs,
        List<String> factsUsed
) {
}