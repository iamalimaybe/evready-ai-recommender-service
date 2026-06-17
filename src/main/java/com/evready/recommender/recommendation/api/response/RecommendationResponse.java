package com.evready.recommender.recommendation.api.response;

import java.util.List;

public record RecommendationResponse(
        Long id,
        String status,
        String summary,
        List<RecommendationItemResponse> recommendations,
        List<String> missingInformation,
        List<String> warnings,
        String validationStatus,
        String failureReason
) {
}