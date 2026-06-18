package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.domain.RecommendationCandidateSnapshot;
import com.evready.recommender.recommendation.domain.RecommendationRun;
import com.evready.recommender.recommendation.domain.RecommendationRunStatus;
import com.evready.recommender.recommendation.domain.RecommendationValidationStatus;
import com.evready.recommender.recommendation.repository.RecommendationCandidateSnapshotRepository;
import com.evready.recommender.recommendation.repository.RecommendationResultRepository;
import com.evready.recommender.recommendation.repository.RecommendationRunRepository;
import com.evready.recommender.recommendation.service.CandidateSelectionService;
import com.evready.recommender.recommendation.service.RecommendationNotFoundException;
import com.evready.recommender.recommendation.service.RecommendationService;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionRecommendation;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final CandidateSelectionService candidateSelectionService;
    private final RecommendationRunRepository runRepository;
    private final RecommendationCandidateSnapshotRepository candidateSnapshotRepository;
    private final RecommendationResultRepository resultRepository;
    private final ObjectMapper objectMapper;
    private final TaskExecutor recommendationTaskExecutor;
    private final RecommendationBackgroundProcessor backgroundProcessor;

    public RecommendationServiceImpl(
            CandidateSelectionService candidateSelectionService,
            RecommendationRunRepository runRepository,
            RecommendationCandidateSnapshotRepository candidateSnapshotRepository,
            RecommendationResultRepository resultRepository,
            ObjectMapper objectMapper,
            @Qualifier("recommendationTaskExecutor") TaskExecutor recommendationTaskExecutor,
            RecommendationBackgroundProcessor backgroundProcessor
    ) {
        this.candidateSelectionService = candidateSelectionService;
        this.runRepository = runRepository;
        this.candidateSnapshotRepository = candidateSnapshotRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
        this.recommendationTaskExecutor = recommendationTaskExecutor;
        this.backgroundProcessor = backgroundProcessor;
    }

    @Override
    @Transactional
    public RecommendationExecutionResult createRecommendation(RecommendationRequest request) {
        CandidateSelectionResult candidateSelection = candidateSelectionService.selectCandidates(request);

        RecommendationRun run = runRepository.save(new RecommendationRun(
                toJson(request),
                RecommendationRunStatus.QUEUED,
                RecommendationValidationStatus.NOT_VALIDATED
        ));

        saveCandidateSnapshots(run.getId(), candidateSelection.candidates());

        run.recordResponseMetadata(
                toJson(candidateSelection.missingInformation()),
                toJson(candidateSelection.warnings())
        );

        if (candidateSelection.candidates().isEmpty()) {
            return completeWithoutModel(run, candidateSelection);
        }

        scheduleAfterCommit(run.getId(), request, candidateSelection);

        return acceptedResult(run, candidateSelection);
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationExecutionResult getRecommendation(Long id) {
        RecommendationRun run = runRepository.findById(id)
                .orElseThrow(() -> new RecommendationNotFoundException(id));

        List<RecommendationExecutionRecommendation> recommendations = resultRepository
                .findByRecommendationRunIdOrderByRankAsc(run.getId())
                .stream()
                .map(result -> new RecommendationExecutionRecommendation(
                        result.getVehicleId(),
                        result.getRank(),
                        result.getMatchReason(),
                        readStringList(result.getTradeoffsJson()),
                        readStringList(result.getFactsUsedJson())
                ))
                .toList();

        return new RecommendationExecutionResult(
                run.getId(),
                run.getStatus(),
                run.getSummary(),
                recommendations,
                readStringList(run.getMissingInformationJson()),
                readStringList(run.getWarningsJson()),
                run.getValidationStatus(),
                run.getFailureReason()
        );
    }

    private RecommendationExecutionResult acceptedResult(
            RecommendationRun run,
            CandidateSelectionResult candidateSelection
    ) {
        return new RecommendationExecutionResult(
                run.getId(),
                RecommendationRunStatus.QUEUED,
                "Recommendation request accepted. Check this id for the generated result.",
                List.of(),
                candidateSelection.missingInformation(),
                candidateSelection.warnings(),
                RecommendationValidationStatus.NOT_VALIDATED,
                null
        );
    }

    private RecommendationExecutionResult completeWithoutModel(
            RecommendationRun run,
            CandidateSelectionResult candidateSelection
    ) {
        String summary = "No catalogue vehicles matched the provided constraints closely enough.";

        run.recordResponseMetadata(
                toJson(candidateSelection.missingInformation()),
                toJson(candidateSelection.warnings())
        );

        run.markCompleted(
                RecommendationRunStatus.INSUFFICIENT_CANDIDATES,
                summary,
                null,
                null,
                RecommendationValidationStatus.VALID
        );

        return new RecommendationExecutionResult(
                run.getId(),
                RecommendationRunStatus.INSUFFICIENT_CANDIDATES,
                summary,
                List.of(),
                candidateSelection.missingInformation(),
                candidateSelection.warnings(),
                RecommendationValidationStatus.VALID,
                null
        );
    }

    private void scheduleAfterCommit(
            Long runId,
            RecommendationRequest request,
            CandidateSelectionResult candidateSelection
    ) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submitBackgroundProcessing(runId, request, candidateSelection);
                }
            });
            return;
        }

        submitBackgroundProcessing(runId, request, candidateSelection);
    }

    private void submitBackgroundProcessing(
            Long runId,
            RecommendationRequest request,
            CandidateSelectionResult candidateSelection
    ) {
        try {
            recommendationTaskExecutor.execute(() ->
                    backgroundProcessor.processRecommendation(runId, request, candidateSelection)
            );
        } catch (TaskRejectedException ex) {
            backgroundProcessor.markQueueRejected(runId);
        }
    }

    private void saveCandidateSnapshots(Long runId, List<CandidateVehicle> candidates) {
        List<RecommendationCandidateSnapshot> snapshots = new ArrayList<>();

        for (int index = 0; index < candidates.size(); index++) {
            CandidateVehicle candidate = candidates.get(index);

            snapshots.add(new RecommendationCandidateSnapshot(
                    runId,
                    candidate.vehicleId(),
                    toJson(candidate),
                    index + 1
            ));
        }

        candidateSnapshotRepository.saveAll(snapshots);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize recommendation data.", ex);
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize stored recommendation list.", ex);
        }
    }
}