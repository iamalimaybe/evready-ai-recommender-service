package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.recommendation.service.RecommendationModelOutputEnricher;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.evready.recommender.recommendation.service.model.RecommendationModelRecommendation;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RecommendationModelOutputEnricherImpl implements RecommendationModelOutputEnricher {

    @Override
    public RecommendationModelOutput enrich(RecommendationModelOutput output) {
        if (output == null || output.recommendations() == null) {
            return output;
        }

        List<RecommendationModelRecommendation> enrichedRecommendations = output.recommendations().stream()
                .map(this::enrichRecommendation)
                .toList();

        return new RecommendationModelOutput(
                output.status(),
                output.summary(),
                enrichedRecommendations,
                output.missingInformation(),
                output.warnings()
        );
    }

    private RecommendationModelRecommendation enrichRecommendation(RecommendationModelRecommendation recommendation) {
        Set<String> factsUsed = new LinkedHashSet<>();

        if (recommendation.factsUsed() != null) {
            factsUsed.addAll(recommendation.factsUsed());
        }

        String recommendationText = collectRecommendationText(recommendation).toLowerCase(Locale.ROOT);

        addFactWhenMentioned(
                recommendationText,
                factsUsed,
                "pricePkr",
                "price",
                "budget",
                "cost",
                "within budget",
                "budget-friendly"
        );

        addFactWhenMentioned(
                recommendationText,
                factsUsed,
                "rangeKm",
                "range",
                "distance",
                "commute",
                "daily drive",
                "daily driving",
                "daily distance"
        );

        addFactWhenMentioned(
                recommendationText,
                factsUsed,
                "dcFastCharging",
                "dc fast",
                "fast charging"
        );

        addFactWhenMentioned(
                recommendationText,
                factsUsed,
                "verificationStatus",
                "verification",
                "verified",
                "unverified",
                "source confidence",
                "catalogue confidence",
                "catalog confidence",
                "source-backed",
                "source-confirmed"
        );

        addFactWhenMentioned(
                recommendationText,
                factsUsed,
                "batteryCapacityKwh",
                "battery capacity",
                "kwh"
        );

        return new RecommendationModelRecommendation(
                recommendation.vehicleId(),
                recommendation.rank(),
                recommendation.matchReason(),
                recommendation.tradeoffs(),
                List.copyOf(factsUsed)
        );
    }

    private void addFactWhenMentioned(
            String recommendationText,
            Set<String> factsUsed,
            String fact,
            String... triggerPhrases
    ) {
        for (String triggerPhrase : triggerPhrases) {
            if (recommendationText.contains(triggerPhrase)) {
                factsUsed.add(fact);
                return;
            }
        }
    }

    private String collectRecommendationText(RecommendationModelRecommendation recommendation) {
        StringBuilder builder = new StringBuilder();

        append(builder, recommendation.matchReason());
        appendAll(builder, recommendation.tradeoffs());

        return builder.toString();
    }

    private void appendAll(StringBuilder builder, List<String> values) {
        if (values == null) {
            return;
        }

        for (String value : values) {
            append(builder, value);
        }
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(' ').append(value);
        }
    }
}