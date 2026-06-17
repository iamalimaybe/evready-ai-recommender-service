package com.evready.recommender.recommendation.service.dto;

import com.evready.recommender.recommendation.domain.RecommendationRunStatus;
import com.evready.recommender.recommendation.domain.RecommendationValidationStatus;

import java.util.List;

public record RecommendationExecutionResult(
        Long id,
        RecommendationRunStatus status,
        String summary,
        List<RecommendationExecutionRecommendation> recommendations,
        List<String> missingInformation,
        List<String> warnings,
        RecommendationValidationStatus validationStatus,
        String failureReason
) {
}