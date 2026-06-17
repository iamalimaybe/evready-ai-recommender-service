package com.evready.recommender.recommendation.api;

import com.evready.recommender.recommendation.api.mapper.RecommendationResponseMapper;
import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.api.response.RecommendationResponse;
import com.evready.recommender.recommendation.service.RecommendationService;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final RecommendationResponseMapper responseMapper;

    public RecommendationController(
            RecommendationService recommendationService,
            RecommendationResponseMapper responseMapper
    ) {
        this.recommendationService = recommendationService;
        this.responseMapper = responseMapper;
    }

    @PostMapping
    public RecommendationResponse createRecommendation(@Valid @RequestBody RecommendationRequest request) {
        RecommendationExecutionResult result = recommendationService.createRecommendation(request);
        return responseMapper.toResponse(result);
    }
}