package com.evready.recommender.recommendation.api.mapper;

import com.evready.recommender.recommendation.api.response.RecommendationItemResponse;
import com.evready.recommender.recommendation.api.response.RecommendationResponse;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionRecommendation;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecommendationResponseMapper {

    public RecommendationResponse toResponse(RecommendationExecutionResult result) {
        return new RecommendationResponse(
                result.id(),
                result.status().name(),
                result.summary(),
                toItemResponses(result.recommendations()),
                result.missingInformation(),
                result.warnings(),
                result.validationStatus().name(),
                result.failureReason()
        );
    }

    private List<RecommendationItemResponse> toItemResponses(List<RecommendationExecutionRecommendation> recommendations) {
        return recommendations.stream()
                .map(this::toItemResponse)
                .toList();
    }

    private RecommendationItemResponse toItemResponse(RecommendationExecutionRecommendation recommendation) {
        return new RecommendationItemResponse(
                recommendation.vehicleId(),
                recommendation.rank(),
                recommendation.matchReason(),
                recommendation.tradeoffs(),
                recommendation.factsUsed()
        );
    }
}