package com.evready.recommender.recommendation.service;

import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;

public interface RecommendationModelOutputEnricher {

    RecommendationModelOutput enrich(RecommendationModelOutput output);
}