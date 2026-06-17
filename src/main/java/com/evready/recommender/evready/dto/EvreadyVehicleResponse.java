package com.evready.recommender.evready.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvreadyVehicleResponse(
        Long id,
        String vehicleType,
        String model,
        String variant,
        BigDecimal pricePkr,
        Integer rangeKm,
        BigDecimal batteryCapacityKwh,
        Boolean dcFastCharging,
        String verificationStatus,
        EvreadyBrandResponse brand,
        EvreadyChargerTypeResponse chargerType
) {
}