package com.evready.recommender.recommendation.repository;

import com.evready.recommender.recommendation.domain.RecommendationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecommendationRunRepository extends JpaRepository<RecommendationRun, Long> {
}