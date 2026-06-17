package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.evready.client.EvreadyCatalogClient;
import com.evready.recommender.evready.dto.EvreadyVehicleResponse;
import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.service.CandidateSelectionService;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CandidateSelectionServiceImpl implements CandidateSelectionService {

    private static final int MAX_CANDIDATES = 5;

    private final EvreadyCatalogClient catalogClient;

    public CandidateSelectionServiceImpl(EvreadyCatalogClient catalogClient) {
        this.catalogClient = catalogClient;
    }

    @Override
    public CandidateSelectionResult selectCandidates(RecommendationRequest request) {
        List<String> missingInformation = collectMissingInformation(request);
        List<String> warnings = collectWarnings(request);

        List<CandidateVehicle> candidates = catalogClient.fetchAllVehicles()
                .stream()
                .filter(vehicle -> matchesVehicleType(vehicle, request.vehicleType()))
                .filter(vehicle -> matchesBudget(vehicle, request.budgetPkr()))
                .filter(vehicle -> canSupportDailyDistance(vehicle, request.dailyDistanceKm()))
                .map(vehicle -> toCandidate(vehicle, request))
                .sorted(candidateComparator(request))
                .limit(MAX_CANDIDATES)
                .toList();

        if (candidates.isEmpty()) {
            warnings.add("No catalogue vehicles matched the current filters closely enough. Try relaxing budget, vehicle type, or distance requirements.");
        }

        return new CandidateSelectionResult(candidates, missingInformation, warnings);
    }

    private List<String> collectMissingInformation(RecommendationRequest request) {
        List<String> missing = new ArrayList<>();

        if (isBlank(request.vehicleType())) {
            missing.add("Vehicle type was not provided.");
        }

        if (request.budgetPkr() == null) {
            missing.add("Budget was not provided.");
        }

        if (request.dailyDistanceKm() == null && request.monthlyDistanceKm() == null) {
            missing.add("Daily or monthly distance was not provided.");
        }

        if (request.homeChargingAvailable() == null) {
            missing.add("Home charging availability was not provided.");
        }

        return missing;
    }

    private List<String> collectWarnings(RecommendationRequest request) {
        List<String> warnings = new ArrayList<>();

        warnings.add("Vehicle prices, specs, range, and availability should be verified before purchase.");

        if (Boolean.FALSE.equals(request.homeChargingAvailable())) {
            warnings.add("Home charging is not available, so public charging access should be checked carefully before choosing an EV.");
        }

        return warnings;
    }

    private boolean matchesVehicleType(EvreadyVehicleResponse vehicle, String requestedVehicleType) {
        if (isBlank(requestedVehicleType)) {
            return true;
        }

        if (isBlank(vehicle.vehicleType())) {
            return false;
        }

        return vehicle.vehicleType().equalsIgnoreCase(requestedVehicleType.trim());
    }

    private boolean matchesBudget(EvreadyVehicleResponse vehicle, BigDecimal budgetPkr) {
        if (budgetPkr == null || vehicle.pricePkr() == null) {
            return true;
        }

        return vehicle.pricePkr().compareTo(budgetPkr) <= 0;
    }

    private boolean canSupportDailyDistance(EvreadyVehicleResponse vehicle, Integer dailyDistanceKm) {
        if (dailyDistanceKm == null || vehicle.rangeKm() == null) {
            return true;
        }

        return vehicle.rangeKm() >= dailyDistanceKm;
    }

    private CandidateVehicle toCandidate(EvreadyVehicleResponse vehicle, RecommendationRequest request) {
        List<String> notes = new ArrayList<>();
        int score = 0;

        if (request.budgetPkr() != null && vehicle.pricePkr() != null) {
            score += scoreBudgetFit(vehicle.pricePkr(), request.budgetPkr());
            notes.add("Vehicle price is within the stated budget.");
        } else if (vehicle.pricePkr() == null) {
            notes.add("Vehicle price is not listed, so budget fit needs manual verification.");
        }

        if (request.dailyDistanceKm() != null && vehicle.rangeKm() != null) {
            score += scoreRangeFit(vehicle.rangeKm(), request.dailyDistanceKm());
            notes.add("Listed range can cover the stated daily distance.");
        } else if (vehicle.rangeKm() == null) {
            notes.add("Vehicle range is not listed, so range fit needs manual verification.");
        }

        if (Boolean.TRUE.equals(vehicle.dcFastCharging())) {
            score += 5;
            notes.add("DC fast charging support is listed.");
        }

        if ("OFFICIAL".equalsIgnoreCase(vehicle.verificationStatus())) {
            score += 10;
            notes.add("Catalogue record is source-backed.");
        } else {
            notes.add("Catalogue record is not source-confirmed.");
        }

        if (Boolean.TRUE.equals(request.homeChargingAvailable())) {
            score += 5;
        }

        return new CandidateVehicle(
                vehicle.id(),
                vehicle.vehicleType(),
                vehicle.brand() == null ? null : vehicle.brand().name(),
                vehicle.model(),
                vehicle.variant(),
                vehicle.pricePkr(),
                vehicle.rangeKm(),
                vehicle.batteryCapacityKwh(),
                vehicle.dcFastCharging(),
                vehicle.verificationStatus(),
                vehicle.chargerType() == null ? null : vehicle.chargerType().code(),
                vehicle.chargerType() == null ? null : vehicle.chargerType().name(),
                score,
                notes
        );
    }

    private int scoreBudgetFit(BigDecimal pricePkr, BigDecimal budgetPkr) {
        if (budgetPkr.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        BigDecimal seventyFivePercent = budgetPkr.multiply(BigDecimal.valueOf(0.75));

        if (pricePkr.compareTo(seventyFivePercent) <= 0) {
            return 30;
        }

        return 20;
    }

    private int scoreRangeFit(Integer rangeKm, Integer dailyDistanceKm) {
        int comfortableRange = dailyDistanceKm * 3;
        int acceptableRange = dailyDistanceKm * 2;

        if (rangeKm >= comfortableRange) {
            return 30;
        }

        if (rangeKm >= acceptableRange) {
            return 20;
        }

        return 10;
    }

    private Comparator<CandidateVehicle> candidateComparator(RecommendationRequest request) {
        Comparator<CandidateVehicle> comparator = Comparator
                .comparingInt(CandidateVehicle::preFilterScore)
                .reversed();

        String priority = normalize(request.priority());

        if (priority.contains("lowest upfront") || priority.contains("low upfront") || priority.contains("cheap")) {
            comparator = comparator.thenComparing(
                    CandidateVehicle::pricePkr,
                    Comparator.nullsLast(BigDecimal::compareTo)
            );
        } else {
            comparator = comparator.thenComparing(
                    CandidateVehicle::rangeKm,
                    Comparator.nullsLast(Comparator.reverseOrder())
            );
        }

        return comparator.thenComparing(CandidateVehicle::vehicleId);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}