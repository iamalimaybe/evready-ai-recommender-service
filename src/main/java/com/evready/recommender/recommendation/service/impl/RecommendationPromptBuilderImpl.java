package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.service.RecommendationPromptBuilder;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationPromptBuilderImpl implements RecommendationPromptBuilder {

    private final ObjectMapper objectMapper;

    public RecommendationPromptBuilderImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String buildPrompt(RecommendationRequest request, List<CandidateVehicle> candidates) {
        return """
                You are an EV recommendation assistant for EVReady Pakistan.

                Your task is to recommend EV options only from the provided candidate vehicles.

                Strict rules:
                - Use only the candidate vehicle facts provided below.
                - Do not invent vehicles.
                - Do not invent prices.
                - Do not invent range values.
                - Do not invent battery capacity.
                - Do not invent charger availability.
                - Do not invent dealer availability.
                - Do not claim live charger status.
                - Do not claim EVReady field-verified a vehicle or charger.
                - Do not give financing, insurance, legal, or tax advice.
                - Be cautious when price, range, source confidence, or charging data may be uncertain.
                - If useful recommendations are not possible, return INSUFFICIENT_CANDIDATES or NEEDS_MORE_INFORMATION.
                - Do not infer route feasibility from range or DC fast charging alone.
                - Do not say a vehicle supports a specific intercity route unless route and charger data are explicitly provided.
                - If the user mentions a route or long trip, say DC fast charging may be useful but route distance, charger availability, connector compatibility, pricing, and access must be checked separately.
                - Do not claim a vehicle can handle Lahore to Islamabad or any other route based only on catalogue facts.

                Allowed status values:
                - ANSWERED
                - INSUFFICIENT_CANDIDATES
                - NEEDS_MORE_INFORMATION

                Allowed factsUsed values:
                - vehicleType
                - brandName
                - model
                - variant
                - pricePkr
                - rangeKm
                - batteryCapacityKwh
                - dcFastCharging
                - verificationStatus
                - chargerTypeCode
                - chargerTypeName
                - preFilterScore
                - selectionNotes

                Return strict JSON only.
                Do not include markdown.
                Do not include explanation outside JSON.

                Required JSON shape:
                {
                  "status": "ANSWERED",
                  "summary": "Short cautious summary.",
                  "recommendations": [
                    {
                      "vehicleId": 1,
                      "rank": 1,
                      "matchReason": "Why this candidate fits based only on provided facts.",
                      "tradeoffs": ["Cautious tradeoff."],
                      "factsUsed": ["pricePkr", "rangeKm"]
                    }
                  ],
                  "missingInformation": [],
                  "warnings": []
                }

                User request:
                %s

                Candidate vehicles:
                %s
                """.formatted(toJson(request), toJson(candidates));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize recommendation prompt data.", ex);
        }
    }
}