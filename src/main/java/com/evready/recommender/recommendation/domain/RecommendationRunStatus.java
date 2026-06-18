package com.evready.recommender.recommendation.domain;

public enum RecommendationRunStatus {
    PENDING,
    QUEUED,
    RUNNING,
    ANSWERED,
    INSUFFICIENT_CANDIDATES,
    NEEDS_MORE_INFORMATION,
    FAILED,
    TIMED_OUT
}