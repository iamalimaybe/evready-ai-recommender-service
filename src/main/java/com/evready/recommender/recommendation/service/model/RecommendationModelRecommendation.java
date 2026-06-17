package com.evready.recommender.recommendation.service.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationModelRecommendation(
        @JsonAlias("vehicle_id")
        Long vehicleId,

        Integer rank,

        @JsonAlias({"match_reason", "reason"})
        String matchReason,

        List<String> tradeoffs,

        @JsonAlias({"facts_used", "facts"})
        List<String> factsUsed
) {
}