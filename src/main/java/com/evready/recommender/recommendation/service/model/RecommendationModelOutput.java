package com.evready.recommender.recommendation.service.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationModelOutput(
        String status,
        String summary,
        List<RecommendationModelRecommendation> recommendations,
        List<String> missingInformation,
        List<String> warnings
) {
}