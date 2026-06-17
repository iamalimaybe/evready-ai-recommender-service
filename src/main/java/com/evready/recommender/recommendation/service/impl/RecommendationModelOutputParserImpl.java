package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.recommendation.service.RecommendationModelOutputParser;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class RecommendationModelOutputParserImpl implements RecommendationModelOutputParser {

    private final ObjectMapper objectMapper;

    public RecommendationModelOutputParserImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public RecommendationModelOutput parse(String rawOutput) {
        if (rawOutput == null || rawOutput.isBlank()) {
            throw new IllegalArgumentException("Model output is blank.");
        }

        try {
            return objectMapper.readValue(rawOutput, RecommendationModelOutput.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Model output is not valid recommendation JSON.", ex);
        }
    }
}