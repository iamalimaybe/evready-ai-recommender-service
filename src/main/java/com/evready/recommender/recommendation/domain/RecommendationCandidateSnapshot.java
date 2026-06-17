package com.evready.recommender.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "recommendation_candidate_snapshot")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationCandidateSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recommendation_run_id", nullable = false)
    private Long recommendationRunId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "candidate_json", nullable = false, columnDefinition = "jsonb")
    private String candidateJson;

    @Column(name = "rank_before_llm")
    private Integer rankBeforeLlm;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecommendationCandidateSnapshot(
            Long recommendationRunId,
            Long vehicleId,
            String candidateJson,
            Integer rankBeforeLlm
    ) {
        this.recommendationRunId = recommendationRunId;
        this.vehicleId = vehicleId;
        this.candidateJson = candidateJson;
        this.rankBeforeLlm = rankBeforeLlm;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecommendationCandidateSnapshot that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}