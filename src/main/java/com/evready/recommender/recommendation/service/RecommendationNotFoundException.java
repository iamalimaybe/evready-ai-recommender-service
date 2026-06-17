package com.evready.recommender.recommendation.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecommendationNotFoundException extends RuntimeException {

    public RecommendationNotFoundException(Long id) {
        super("Recommendation run not found: " + id);
    }
}