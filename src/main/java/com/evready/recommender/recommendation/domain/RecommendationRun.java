package com.evready.recommender.recommendation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(name = "recommendation_run")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "request_json", nullable = false, columnDefinition = "jsonb")
    private String requestJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private RecommendationRunStatus status;

    @Column(name = "summary")
    private String summary;

    @Column(name = "raw_model_output")
    private String rawModelOutput;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parsed_output_json", columnDefinition = "jsonb")
    private String parsedOutputJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", nullable = false, length = 50)
    private RecommendationValidationStatus validationStatus;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "model_name", length = 120)
    private String modelName;

    @Column(name = "model_provider", length = 80)
    private String modelProvider;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "run_config_json", columnDefinition = "jsonb")
    private String runConfigJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public RecommendationRun(
            String requestJson,
            RecommendationRunStatus status,
            RecommendationValidationStatus validationStatus
    ) {
        this.requestJson = requestJson;
        this.status = status;
        this.validationStatus = validationStatus;
    }

    public void markRunning(String modelProvider, String modelName, BigDecimal temperature, String runConfigJson) {
        this.status = RecommendationRunStatus.RUNNING;
        this.modelProvider = modelProvider;
        this.modelName = modelName;
        this.temperature = temperature;
        this.runConfigJson = runConfigJson;
    }

    public void markCompleted(
            RecommendationRunStatus status,
            String summary,
            String rawModelOutput,
            String parsedOutputJson,
            RecommendationValidationStatus validationStatus
    ) {
        this.status = status;
        this.summary = summary;
        this.rawModelOutput = rawModelOutput;
        this.parsedOutputJson = parsedOutputJson;
        this.validationStatus = validationStatus;
        this.failureReason = null;
    }

    public void markFailed(String failureReason, String rawModelOutput) {
        this.status = RecommendationRunStatus.FAILED;
        this.validationStatus = RecommendationValidationStatus.INVALID;
        this.failureReason = failureReason;
        this.rawModelOutput = rawModelOutput;
    }

    public void recordModelMetadata(String modelProvider, String modelName, BigDecimal temperature, String runConfigJson) {
        this.modelProvider = modelProvider;
        this.modelName = modelName;
        this.temperature = temperature;
        this.runConfigJson = runConfigJson;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RecommendationRun that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}