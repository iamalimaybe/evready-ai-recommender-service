package com.evready.recommender.recommendation.service;

import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;

public interface CandidateSelectionService {

    CandidateSelectionResult selectCandidates(RecommendationRequest request);
}