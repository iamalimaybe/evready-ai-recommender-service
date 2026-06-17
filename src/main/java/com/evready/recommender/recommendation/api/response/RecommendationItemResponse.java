package com.evready.recommender.recommendation.api.response;

import java.util.List;

public record RecommendationItemResponse(
        Long vehicleId,
        Integer rank,
        String matchReason,
        List<String> tradeoffs,
        List<String> factsUsed
) {
}