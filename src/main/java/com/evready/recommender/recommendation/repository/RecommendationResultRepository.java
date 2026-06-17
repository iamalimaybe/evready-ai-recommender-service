package com.evready.recommender.recommendation.repository;

import com.evready.recommender.recommendation.domain.RecommendationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    List<RecommendationResult> findByRecommendationRunIdOrderByRankAsc(Long recommendationRunId);
}