package com.evready.recommender.recommendation.service.validation;

import com.evready.recommender.recommendation.domain.RecommendationRunStatus;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.evready.recommender.recommendation.service.model.RecommendationModelRecommendation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationModelOutputValidatorImplTest {

    private final RecommendationModelOutputValidatorImpl validator = new RecommendationModelOutputValidatorImpl();

    @Test
    void validateReturnsValidForSafeAnsweredOutput() {
        RecommendationModelOutput output = new RecommendationModelOutput(
                "ANSWERED",
                "Three EVs fit the request, but longer trips require separate checks.",
                List.of(new RecommendationModelRecommendation(
                        66L,
                        1,
                        "Price within budget, range covers daily commute, supports DC fast charging.",
                        List.of("Unverified catalogue data; range may vary in real-world use."),
                        List.of("pricePkr", "rangeKm", "dcFastCharging", "verificationStatus")
                )),
                List.of(),
                List.of("Route distance, charger availability, connector compatibility, pricing, and access should be checked separately.")
        );

        RecommendationOutputValidationResult result = validator.validate(output, List.of(candidate(66L)));

        assertThat(result.valid()).isTrue();
        assertThat(result.status()).isEqualTo(RecommendationRunStatus.ANSWERED);
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void validateRejectsRecommendationForVehicleOutsideCandidateList() {
        RecommendationModelOutput output = new RecommendationModelOutput(
                "ANSWERED",
                "Summary.",
                List.of(new RecommendationModelRecommendation(
                        999L,
                        1,
                        "Price within budget and range covers daily commute.",
                        List.of("Unverified catalogue data."),
                        List.of("pricePkr", "rangeKm", "verificationStatus")
                )),
                List.of(),
                List.of()
        );

        RecommendationOutputValidationResult result = validator.validate(output, List.of(candidate(66L)));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .contains("Recommended vehicleId was not in candidate list: 999");
    }

    @Test
    void validateRejectsMissingFactsUsedCoverage() {
        RecommendationModelOutput output = new RecommendationModelOutput(
                "ANSWERED",
                "Summary.",
                List.of(new RecommendationModelRecommendation(
                        66L,
                        1,
                        "Price within budget, range covers daily commute, supports DC fast charging.",
                        List.of(),
                        List.of("pricePkr", "rangeKm")
                )),
                List.of(),
                List.of()
        );

        RecommendationOutputValidationResult result = validator.validate(output, List.of(candidate(66L)));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .contains("Recommendation for vehicleId 66 mentions DC fast charging or fast charging but factsUsed does not include dcFastCharging.");
    }

    @Test
    void validateRejectsDcFastChargingAvailableWording() {
        RecommendationModelOutput output = new RecommendationModelOutput(
                "ANSWERED",
                "Summary.",
                List.of(new RecommendationModelRecommendation(
                        66L,
                        1,
                        "Price within budget, range covers daily commute, DC fast charging available.",
                        List.of(),
                        List.of("pricePkr", "rangeKm", "dcFastCharging")
                )),
                List.of(),
                List.of()
        );

        RecommendationOutputValidationResult result = validator.validate(output, List.of(candidate(66L)));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors())
                .contains("Unsupported claim detected: dc fast charging available");
    }

    private CandidateVehicle candidate(Long vehicleId) {
        return new CandidateVehicle(
                vehicleId,
                "CAR",
                "GUGO",
                "Box",
                "E3",
                BigDecimal.valueOf(6650000),
                430,
                BigDecimal.valueOf(42.3),
                true,
                "UNVERIFIED",
                "CCS2",
                "CCS Type 2",
                90,
                List.of("Price within budget.", "Range covers daily distance.")
        );
    }
}