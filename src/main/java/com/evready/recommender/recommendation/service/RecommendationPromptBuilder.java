package com.evready.recommender.recommendation.service;

import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;

import java.util.List;

public interface RecommendationPromptBuilder {

    String buildPrompt(RecommendationRequest request, List<CandidateVehicle> candidates);
}