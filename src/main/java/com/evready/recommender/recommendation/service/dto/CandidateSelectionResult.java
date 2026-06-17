package com.evready.recommender.recommendation.service.dto;

import java.util.List;

public record CandidateSelectionResult(
        List<CandidateVehicle> candidates,
        List<String> missingInformation,
        List<String> warnings
) {
}