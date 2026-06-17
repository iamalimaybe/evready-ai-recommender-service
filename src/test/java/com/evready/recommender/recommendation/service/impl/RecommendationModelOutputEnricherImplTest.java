package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.evready.recommender.recommendation.service.model.RecommendationModelRecommendation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationModelOutputEnricherImplTest {

    private final RecommendationModelOutputEnricherImpl enricher = new RecommendationModelOutputEnricherImpl();

    @Test
    void enrichAddsFactsUsedBasedOnRecommendationText() {
        RecommendationModelOutput output = new RecommendationModelOutput(
                "ANSWERED",
                "Summary.",
                List.of(new RecommendationModelRecommendation(
                        66L,
                        1,
                        "Price within budget, range covers daily commute, supports DC fast charging.",
                        List.of("Unverified catalogue data; battery capacity is lower."),
                        List.of("pricePkr", "rangeKm")
                )),
                List.of(),
                List.of()
        );

        RecommendationModelOutput enriched = enricher.enrich(output);

        assertThat(enriched.recommendations()).hasSize(1);
        assertThat(enriched.recommendations().get(0).factsUsed())
                .containsExactly(
                        "pricePkr",
                        "rangeKm",
                        "dcFastCharging",
                        "verificationStatus",
                        "batteryCapacityKwh"
                );
    }
}