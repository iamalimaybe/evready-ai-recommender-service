package com.evready.recommender.recommendation.api;

import com.evready.recommender.recommendation.api.mapper.CandidateSelectionResponseMapper;
import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.api.response.CandidateSelectionResponse;
import com.evready.recommender.recommendation.service.CandidateSelectionService;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
public class CandidateSelectionController {

    private final CandidateSelectionService candidateSelectionService;
    private final CandidateSelectionResponseMapper responseMapper;

    public CandidateSelectionController(
            CandidateSelectionService candidateSelectionService,
            CandidateSelectionResponseMapper responseMapper
    ) {
        this.candidateSelectionService = candidateSelectionService;
        this.responseMapper = responseMapper;
    }

    @PostMapping("/candidates")
    public CandidateSelectionResponse selectCandidates(@Valid @RequestBody RecommendationRequest request) {
        CandidateSelectionResult result = candidateSelectionService.selectCandidates(request);
        return responseMapper.toResponse(result);
    }
}