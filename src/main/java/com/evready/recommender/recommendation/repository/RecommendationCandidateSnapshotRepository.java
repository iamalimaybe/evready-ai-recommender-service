package com.evready.recommender.recommendation.repository;

import com.evready.recommender.recommendation.domain.RecommendationCandidateSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationCandidateSnapshotRepository extends JpaRepository<RecommendationCandidateSnapshot, Long> {

    List<RecommendationCandidateSnapshot> findByRecommendationRunIdOrderByRankBeforeLlmAscIdAsc(Long recommendationRunId);
}