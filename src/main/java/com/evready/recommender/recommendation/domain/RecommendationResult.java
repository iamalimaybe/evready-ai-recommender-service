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
@Table(name = "recommendation_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recommendation_run_id", nullable = false)
    private Long recommendationRunId;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column(name = "rank", nullable = false)
    private Integer rank;

    @Column(name = "match_reason", nullable = false)
    private String matchReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tradeoffs_json", nullable = false, columnDefinition = "jsonb")
    private String tradeoffsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "facts_used_json", nullable = false, columnDefinition = "jsonb")
    private String factsUsedJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RecommendationResult(
            Long recommendationRunId,
            Long vehicleId,
            Integer rank,
            String matchReason,
            String tradeoffsJson,
            String factsUsedJson
    ) {
        this.recommendationRunId = recommendationRunId;
        this.vehicleId = vehicleId;
        this.rank = rank;
        this.matchReason = matchReason;
        this.tradeoffsJson = tradeoffsJson;
        this.factsUsedJson = factsUsedJson;
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
        if (!(other instanceof RecommendationResult that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}