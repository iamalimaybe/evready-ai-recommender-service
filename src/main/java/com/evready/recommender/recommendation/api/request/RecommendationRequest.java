package com.evready.recommender.recommendation.api.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RecommendationRequest(

        @Size(max = 20)
        String vehicleType,

        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal budgetPkr,

        @Size(max = 100)
        String city,

        @Min(1)
        Integer dailyDistanceKm,

        @Min(1)
        Integer monthlyDistanceKm,

        Boolean homeChargingAvailable,

        Boolean solarAvailable,

        @Size(max = 160)
        String primaryUseCase,

        @Min(1)
        Integer familySize,

        @Size(max = 120)
        String priority,

        @Size(max = 1000)
        String additionalNotes
) {
}