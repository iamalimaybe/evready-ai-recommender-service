package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.llm.dto.ModelGenerationRequest;
import com.evready.recommender.llm.dto.ModelGenerationResponse;
import com.evready.recommender.llm.service.ModelGenerationClient;
import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.domain.RecommendationCandidateSnapshot;
import com.evready.recommender.recommendation.domain.RecommendationResult;
import com.evready.recommender.recommendation.domain.RecommendationRun;
import com.evready.recommender.recommendation.domain.RecommendationRunStatus;
import com.evready.recommender.recommendation.domain.RecommendationValidationStatus;
import com.evready.recommender.recommendation.repository.RecommendationCandidateSnapshotRepository;
import com.evready.recommender.recommendation.repository.RecommendationResultRepository;
import com.evready.recommender.recommendation.repository.RecommendationRunRepository;
import com.evready.recommender.recommendation.service.*;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;
import com.evready.recommender.recommendation.service.dto.CandidateVehicle;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionRecommendation;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionResult;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.evready.recommender.recommendation.service.model.RecommendationModelRecommendation;
import com.evready.recommender.recommendation.service.validation.RecommendationModelOutputValidator;
import com.evready.recommender.recommendation.service.validation.RecommendationOutputValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final CandidateSelectionService candidateSelectionService;
    private final RecommendationPromptBuilder promptBuilder;
    private final ModelGenerationClient modelGenerationClient;
    private final RecommendationModelOutputParser outputParser;
    private final RecommendationModelOutputValidator outputValidator;
    private final RecommendationRunRepository runRepository;
    private final RecommendationCandidateSnapshotRepository candidateSnapshotRepository;
    private final RecommendationResultRepository resultRepository;
    private final ObjectMapper objectMapper;
    private final RecommendationModelOutputEnricher outputEnricher;

    public RecommendationServiceImpl(
            CandidateSelectionService candidateSelectionService,
            RecommendationPromptBuilder promptBuilder,
            ModelGenerationClient modelGenerationClient,
            RecommendationModelOutputParser outputParser,
            RecommendationModelOutputValidator outputValidator,
            RecommendationRunRepository runRepository,
            RecommendationCandidateSnapshotRepository candidateSnapshotRepository,
            RecommendationResultRepository resultRepository,
            ObjectMapper objectMapper,
            RecommendationModelOutputEnricher outputEnricher
    ) {
        this.candidateSelectionService = candidateSelectionService;
        this.promptBuilder = promptBuilder;
        this.modelGenerationClient = modelGenerationClient;
        this.outputParser = outputParser;
        this.outputValidator = outputValidator;
        this.runRepository = runRepository;
        this.candidateSnapshotRepository = candidateSnapshotRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
        this.outputEnricher = outputEnricher;
    }

    @Override
    @Transactional
    public RecommendationExecutionResult createRecommendation(RecommendationRequest request) {
        CandidateSelectionResult candidateSelection = candidateSelectionService.selectCandidates(request);

        RecommendationRun run = runRepository.save(new RecommendationRun(
                toJson(request),
                RecommendationRunStatus.PENDING,
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

        String prompt = promptBuilder.buildPrompt(request, candidateSelection.candidates());
        String rawModelOutput = null;

        try {
            ModelGenerationResponse modelResponse = modelGenerationClient.generate(new ModelGenerationRequest(prompt));
            rawModelOutput = modelResponse.rawOutput();

            run.recordModelMetadata(
                    modelResponse.provider(),
                    modelResponse.modelName(),
                    modelResponse.temperature(),
                    modelResponse.runConfigJson()
            );

            RecommendationModelOutput parsedOutput = outputParser.parse(rawModelOutput);
            RecommendationModelOutput output = outputEnricher.enrich(parsedOutput);

            RecommendationOutputValidationResult validationResult = outputValidator.validate(
                    output,
                    candidateSelection.candidates()
            );

            if (!validationResult.valid()) {
                String failureReason = String.join("; ", validationResult.errors());
                run.markFailed(failureReason, rawModelOutput);
                return failedResult(run, candidateSelection, failureReason);
            }

            List<String> missingInformation = mergeDistinct(
                    candidateSelection.missingInformation(),
                    output.missingInformation()
            );

            List<String> warnings = mergeDistinct(
                    candidateSelection.warnings(),
                    output.warnings()
            );

            run.recordResponseMetadata(
                    toJson(missingInformation),
                    toJson(warnings)
            );

            run.markCompleted(
                    validationResult.status(),
                    output.summary(),
                    rawModelOutput,
                    toJson(output),
                    RecommendationValidationStatus.VALID
            );

            List<RecommendationExecutionRecommendation> recommendations = saveRecommendationResults(
                    run.getId(),
                    output.recommendations()
            );

            return new RecommendationExecutionResult(
                    run.getId(),
                    validationResult.status(),
                    output.summary(),
                    recommendations,
                    missingInformation,
                    warnings,
                    RecommendationValidationStatus.VALID,
                    null
            );
        } catch (RuntimeException ex) {
            String failureReason = safeFailureReason(ex);
            run.markFailed(failureReason, rawModelOutput);
            return failedResult(run, candidateSelection, failureReason);
        }
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

    private List<RecommendationExecutionRecommendation> saveRecommendationResults(
            Long runId,
            List<RecommendationModelRecommendation> modelRecommendations
    ) {
        if (modelRecommendations == null || modelRecommendations.isEmpty()) {
            return List.of();
        }

        List<RecommendationResult> entities = modelRecommendations.stream()
                .map(recommendation -> new RecommendationResult(
                        runId,
                        recommendation.vehicleId(),
                        recommendation.rank(),
                        recommendation.matchReason(),
                        toJson(recommendation.tradeoffs()),
                        toJson(recommendation.factsUsed())
                ))
                .toList();

        resultRepository.saveAll(entities);

        return modelRecommendations.stream()
                .map(recommendation -> new RecommendationExecutionRecommendation(
                        recommendation.vehicleId(),
                        recommendation.rank(),
                        recommendation.matchReason(),
                        recommendation.tradeoffs(),
                        recommendation.factsUsed()
                ))
                .toList();
    }

    private RecommendationExecutionResult failedResult(
            RecommendationRun run,
            CandidateSelectionResult candidateSelection,
            String failureReason
    ) {
        return new RecommendationExecutionResult(
                run.getId(),
                RecommendationRunStatus.FAILED,
                "The recommendation could not be completed safely.",
                List.of(),
                candidateSelection.missingInformation(),
                candidateSelection.warnings(),
                RecommendationValidationStatus.INVALID,
                failureReason
        );
    }

    private List<String> mergeDistinct(List<String> first, List<String> second) {
        Set<String> merged = new LinkedHashSet<>();

        if (first != null) {
            merged.addAll(first);
        }

        if (second != null) {
            merged.addAll(second);
        }

        return List.copyOf(merged);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize recommendation data.", ex);
        }
    }

    private String safeFailureReason(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            return ex.getClass().getSimpleName();
        }

        return ex.getMessage();
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