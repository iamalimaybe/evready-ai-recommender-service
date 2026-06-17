package com.evready.recommender.recommendation.domain;

public enum RecommendationRunStatus {
    PENDING,
    RUNNING,
    ANSWERED,
    INSUFFICIENT_CANDIDATES,
    NEEDS_MORE_INFORMATION,
    FAILED
}