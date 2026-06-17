package com.evready.recommender.recommendation.api.response;

import java.util.List;

public record CandidateSelectionResponse(
        List<CandidateVehicleResponse> candidates,
        List<String> missingInformation,
        List<String> warnings
) {
}