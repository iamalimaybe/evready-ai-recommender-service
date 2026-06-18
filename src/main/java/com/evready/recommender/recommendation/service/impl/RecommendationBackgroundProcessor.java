package com.evready.recommender.recommendation.service.impl;

import com.evready.recommender.llm.dto.ModelGenerationRequest;
import com.evready.recommender.llm.dto.ModelGenerationResponse;
import com.evready.recommender.llm.service.ModelGenerationClient;
import com.evready.recommender.llm.service.ModelGenerationTimeoutException;
import com.evready.recommender.recommendation.api.request.RecommendationRequest;
import com.evready.recommender.recommendation.domain.RecommendationResult;
import com.evready.recommender.recommendation.domain.RecommendationRun;
import com.evready.recommender.recommendation.domain.RecommendationRunStatus;
import com.evready.recommender.recommendation.domain.RecommendationValidationStatus;
import com.evready.recommender.recommendation.repository.RecommendationResultRepository;
import com.evready.recommender.recommendation.repository.RecommendationRunRepository;
import com.evready.recommender.recommendation.service.RecommendationModelOutputEnricher;
import com.evready.recommender.recommendation.service.RecommendationModelOutputParser;
import com.evready.recommender.recommendation.service.RecommendationNotFoundException;
import com.evready.recommender.recommendation.service.RecommendationPromptBuilder;
import com.evready.recommender.recommendation.service.dto.CandidateSelectionResult;
import com.evready.recommender.recommendation.service.dto.RecommendationExecutionRecommendation;
import com.evready.recommender.recommendation.service.model.RecommendationModelOutput;
import com.evready.recommender.recommendation.service.model.RecommendationModelRecommendation;
import com.evready.recommender.recommendation.service.validation.RecommendationModelOutputValidator;
import com.evready.recommender.recommendation.service.validation.RecommendationOutputValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class RecommendationBackgroundProcessor {

    private final RecommendationPromptBuilder promptBuilder;
    private final ModelGenerationClient modelGenerationClient;
    private final RecommendationModelOutputParser outputParser;
    private final RecommendationModelOutputValidator outputValidator;
    private final RecommendationRunRepository runRepository;
    private final RecommendationResultRepository resultRepository;
    private final ObjectMapper objectMapper;
    private final RecommendationModelOutputEnricher outputEnricher;

    public RecommendationBackgroundProcessor(
            RecommendationPromptBuilder promptBuilder,
            ModelGenerationClient modelGenerationClient,
            RecommendationModelOutputParser outputParser,
            RecommendationModelOutputValidator outputValidator,
            RecommendationRunRepository runRepository,
            RecommendationResultRepository resultRepository,
            ObjectMapper objectMapper,
            RecommendationModelOutputEnricher outputEnricher
    ) {
        this.promptBuilder = promptBuilder;
        this.modelGenerationClient = modelGenerationClient;
        this.outputParser = outputParser;
        this.outputValidator = outputValidator;
        this.runRepository = runRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
        this.outputEnricher = outputEnricher;
    }

    public void processRecommendation(
            Long runId,
            RecommendationRequest request,
            CandidateSelectionResult candidateSelection
    ) {
        String rawModelOutput = null;
        markRunning(runId);

        try {
            String prompt = promptBuilder.buildPrompt(request, candidateSelection.candidates());

            ModelGenerationResponse modelResponse = modelGenerationClient.generate(new ModelGenerationRequest(prompt));
            rawModelOutput = modelResponse.rawOutput();

            RecommendationModelOutput parsedOutput = outputParser.parse(rawModelOutput);
            RecommendationModelOutput output = outputEnricher.enrich(parsedOutput);

            RecommendationOutputValidationResult validationResult = outputValidator.validate(
                    output,
                    candidateSelection.candidates()
            );

            if (!validationResult.valid()) {
                String failureReason = String.join("; ", validationResult.errors());
                markFailed(runId, failureReason, rawModelOutput);
                return;
            }

            List<String> missingInformation = mergeDistinct(
                    candidateSelection.missingInformation(),
                    output.missingInformation()
            );

            List<String> warnings = mergeDistinct(
                    candidateSelection.warnings(),
                    output.warnings()
            );

            saveRecommendationResults(runId, output.recommendations());

            RecommendationRun run = getRun(runId);
            run.recordModelMetadata(
                    modelResponse.provider(),
                    modelResponse.modelName(),
                    modelResponse.temperature(),
                    modelResponse.runConfigJson()
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

            runRepository.save(run);
        } catch (ModelGenerationTimeoutException ex) {
            markTimedOut(runId, safeFailureReason(ex), rawModelOutput);
        } catch (RuntimeException ex) {
            markFailed(runId, safeFailureReason(ex), rawModelOutput);
        }
    }

    public void markQueueRejected(Long runId) {
        markFailed(
                runId,
                "Recommendation queue is currently full. Please try again later.",
                null
        );
    }

    private void markRunning(Long runId) {
        RecommendationRun run = getRun(runId);
        run.markRunning();
        runRepository.save(run);
    }

    private void markFailed(Long runId, String failureReason, String rawModelOutput) {
        RecommendationRun run = getRun(runId);
        run.markFailed(failureReason, rawModelOutput);
        runRepository.save(run);
    }

    private void markTimedOut(Long runId, String failureReason, String rawModelOutput) {
        RecommendationRun run = getRun(runId);
        run.markTimedOut(failureReason, rawModelOutput);
        runRepository.save(run);
    }

    private RecommendationRun getRun(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new RecommendationNotFoundException(runId));
    }

    private void saveRecommendationResults(
            Long runId,
            List<RecommendationModelRecommendation> modelRecommendations
    ) {
        if (modelRecommendations == null || modelRecommendations.isEmpty()) {
            return;
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
}