package com.evready.recommender.recommendation.service.dto;

import java.math.BigDecimal;
import java.util.List;

public record CandidateVehicle(
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