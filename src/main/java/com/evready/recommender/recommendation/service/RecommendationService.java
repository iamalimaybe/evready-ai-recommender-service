package com.evready.recommender.recommendation.service;

import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionResult;

public interface RecommendationService {

    RecommendationExecutionResult createRecommendation(RecommendationRequest request);

    RecommendationExecutionResult getRecommendation(Long id);
}