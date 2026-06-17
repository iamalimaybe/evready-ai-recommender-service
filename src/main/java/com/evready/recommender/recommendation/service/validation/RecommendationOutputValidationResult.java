package com.evready.recommender.recommendation.service.validation;

import com.evready.recommender.recommendation.domain.RecommendationRunStatus;

import java.util.List;

public record RecommendationOutputValidationResult(
        boolean valid,
        RecommendationRunStatus status,
        List<String> errors
) {

    public static RecommendationOutputValidationResult valid(RecommendationRunStatus status) {
        return new RecommendationOutputValidationResult(true, status, List.of());
    }

    public static RecommendationOutputValidationResult invalid(List<String> errors) {
        return new RecommendationOutputValidationResult(false, RecommendationRunStatus.FAILED, errors);
    }
}