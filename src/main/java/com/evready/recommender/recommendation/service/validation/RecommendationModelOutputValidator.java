package com.evready.recommender.recommendation.service.validation;

import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;

import java.util.List;

public interface RecommendationModelOutputValidator {

    RecommendationOutputValidationResult validate(
            RecommendationModelOutput output,
            List<CandidateVehicle> candidates
    );
}