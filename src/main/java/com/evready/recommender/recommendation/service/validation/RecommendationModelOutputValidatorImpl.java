package com.evready.recommender.recommendation.service.validation;

import com.evready.recommender.recommendation.domain.RecommendationRunStatus;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.evready.recommender.recommendation.service.model.RecommendationModelRecommendation;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class RecommendationModelOutputValidatorImpl implements RecommendationModelOutputValidator {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "ANSWERED",
            "INSUFFICIENT_CANDIDATES",
            "NEEDS_MORE_INFORMATION"
    );

    private static final Set<String> ALLOWED_FACTS_USED = Set.of(
            "vehicleType",
            "brandName",
            "model",
            "variant",
            "pricePkr",
            "rangeKm",
            "batteryCapacityKwh",
            "dcFastCharging",
            "verificationStatus",
            "chargerTypeCode",
            "chargerTypeName",
            "preFilterScore",
            "selectionNotes"
    );

    private static final List<String> UNSUPPORTED_CLAIM_PHRASES = List.of(
            "live charger availability",
            "guaranteed charger availability",
            "available right now",
            "currently available",
            "charger is available",
            "charger will be available",
            "guaranteed route",
            "route is guaranteed",
            "dealer availability",
            "dealer has availability",
            "in stock",
            "confirmed stock",
            "evready verified",
            "evready field verified",
            "field verified",
            "physically verified",
            "physically audited",
            "guaranteed price",
            "confirmed current price",
            "definitely available"
    );

    @Override
    public RecommendationOutputValidationResult validate(
            RecommendationModelOutput output,
            List<CandidateVehicle> candidates
    ) {
        List<String> errors = new ArrayList<>();

        if (output == null) {
            return RecommendationOutputValidationResult.invalid(List.of("Model output is null."));
        }

        validateRequiredFields(output, errors);

        RecommendationRunStatus status = resolveStatus(output.status(), errors);

        if (output.recommendations() != null) {
            validateRecommendations(output, candidates, status, errors);
        }

        validateUnsupportedClaims(output, errors);

        if (!errors.isEmpty()) {
            return RecommendationOutputValidationResult.invalid(errors);
        }

        return RecommendationOutputValidationResult.valid(status);
    }

    private void validateRequiredFields(RecommendationModelOutput output, List<String> errors) {
        if (isBlank(output.status())) {
            errors.add("status is required.");
        }

        if (isBlank(output.summary())) {
            errors.add("summary is required.");
        }

        if (output.recommendations() == null) {
            errors.add("recommendations is required.");
        }

        if (output.missingInformation() == null) {
            errors.add("missingInformation is required.");
        }

        if (output.warnings() == null) {
            errors.add("warnings is required.");
        }
    }

    private RecommendationRunStatus resolveStatus(String rawStatus, List<String> errors) {
        if (isBlank(rawStatus)) {
            return RecommendationRunStatus.FAILED;
        }

        String normalizedStatus = rawStatus.trim().toUpperCase(Locale.ROOT);

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            errors.add("Unsupported status: " + rawStatus);
            return RecommendationRunStatus.FAILED;
        }

        return RecommendationRunStatus.valueOf(normalizedStatus);
    }

    private void validateRecommendations(
            RecommendationModelOutput output,
            List<CandidateVehicle> candidates,
            RecommendationRunStatus status,
            List<String> errors
    ) {
        List<RecommendationModelRecommendation> recommendations = output.recommendations();

        if (status == RecommendationRunStatus.ANSWERED && recommendations.isEmpty()) {
            errors.add("ANSWERED output must include at least one recommendation.");
        }

        if ((status == RecommendationRunStatus.INSUFFICIENT_CANDIDATES
                || status == RecommendationRunStatus.NEEDS_MORE_INFORMATION)
                && !recommendations.isEmpty()) {
            errors.add(status + " output must not include recommendations.");
        }

        Set<Long> candidateVehicleIds = candidateVehicleIds(candidates);
        Set<Integer> ranks = new HashSet<>();

        for (RecommendationModelRecommendation recommendation : recommendations) {
            validateRecommendation(recommendation, candidateVehicleIds, ranks, errors);
        }
    }

    private void validateRecommendation(
            RecommendationModelRecommendation recommendation,
            Set<Long> candidateVehicleIds,
            Set<Integer> ranks,
            List<String> errors
    ) {
        if (recommendation.vehicleId() == null) {
            errors.add("Recommendation vehicleId is required.");
        } else if (!candidateVehicleIds.contains(recommendation.vehicleId())) {
            errors.add("Recommended vehicleId was not in candidate list: " + recommendation.vehicleId());
        }

        if (recommendation.rank() == null) {
            errors.add("Recommendation rank is required.");
        } else if (!ranks.add(recommendation.rank())) {
            errors.add("Recommendation rank is duplicated: " + recommendation.rank());
        }

        if (isBlank(recommendation.matchReason())) {
            errors.add("Recommendation matchReason is required.");
        }

        if (recommendation.tradeoffs() == null) {
            errors.add("Recommendation tradeoffs is required.");
        }

        if (recommendation.factsUsed() == null) {
            errors.add("Recommendation factsUsed is required.");
        } else {
            for (String factUsed : recommendation.factsUsed()) {
                if (!ALLOWED_FACTS_USED.contains(factUsed)) {
                    errors.add("Unsupported factsUsed value: " + factUsed);
                }
            }
        }
    }

    private Set<Long> candidateVehicleIds(List<CandidateVehicle> candidates) {
        Set<Long> ids = new HashSet<>();

        if (candidates == null) {
            return ids;
        }

        for (CandidateVehicle candidate : candidates) {
            if (candidate.vehicleId() != null) {
                ids.add(candidate.vehicleId());
            }
        }

        return ids;
    }

    private void validateUnsupportedClaims(RecommendationModelOutput output, List<String> errors) {
        String combinedText = collectOutputText(output).toLowerCase(Locale.ROOT);

        for (String phrase : UNSUPPORTED_CLAIM_PHRASES) {
            if (combinedText.contains(phrase)) {
                errors.add("Unsupported claim detected: " + phrase);
            }
        }
    }

    private String collectOutputText(RecommendationModelOutput output) {
        StringBuilder builder = new StringBuilder();

        append(builder, output.summary());
        appendAll(builder, output.missingInformation());
        appendAll(builder, output.warnings());

        if (output.recommendations() != null) {
            for (RecommendationModelRecommendation recommendation : output.recommendations()) {
                append(builder, recommendation.matchReason());
                appendAll(builder, recommendation.tradeoffs());
                appendAll(builder, recommendation.factsUsed());
            }
        }

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
        if (!isBlank(value)) {
            builder.append(' ').append(value);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}