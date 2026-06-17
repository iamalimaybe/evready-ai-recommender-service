package com.evready.recommender.recommendation.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationModelRecommendation(
        Long vehicleId,
        Integer rank,
        String matchReason,
        List<String> tradeoffs,
        List<String> factsUsed
) {
}