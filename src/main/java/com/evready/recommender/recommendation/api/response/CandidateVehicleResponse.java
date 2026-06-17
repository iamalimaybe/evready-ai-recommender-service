package com.evready.recommender.recommendation.api.response;

import java.math.BigDecimal;
import java.util.List;

public record CandidateVehicleResponse(
        Long vehicleId,
        String vehicleType,
        String brandName,
        String model,
        String variant,
        BigDecimal pricePkr,
        Integer rangeKm,
        BigDecimal batteryCapacityKwh,
        Boolean dcFastCharging,
        String verificationStatus,
        String chargerTypeCode,
        String chargerTypeName,
        int preFilterScore,
        List<String> selectionNotes
) {
}