package com.evready.recommender.recommendation.api.mapper;

import com.evready.recommender.recommendation.api.response.CandidateSelectionResponse;
import com.evready.recommender.recommendation.api.response.CandidateVehicleResponse;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CandidateSelectionResponseMapper {

    public CandidateSelectionResponse toResponse(CandidateSelectionResult result) {
        return new CandidateSelectionResponse(
                toCandidateResponses(result.candidates()),
                result.missingInformation(),
                result.warnings()
        );
    }

    private List<CandidateVehicleResponse> toCandidateResponses(List<CandidateVehicle> candidates) {
        return candidates.stream()
                .map(this::toCandidateResponse)
                .toList();
    }

    private CandidateVehicleResponse toCandidateResponse(CandidateVehicle candidate) {
        return new CandidateVehicleResponse(
                candidate.vehicleId(),
                candidate.vehicleType(),
                candidate.brandName(),
                candidate.model(),
                candidate.variant(),
                candidate.pricePkr(),
                candidate.rangeKm(),
                candidate.batteryCapacityKwh(),
                candidate.dcFastCharging(),
                candidate.verificationStatus(),
                candidate.chargerTypeCode(),
                candidate.chargerTypeName(),
                candidate.preFilterScore(),
                candidate.selectionNotes()
        );
    }
}