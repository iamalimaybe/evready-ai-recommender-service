package com.evready.recommender.llm.service.impl;

import com.evready.recommender.llm.config.LlmProperties;
import com.evready.recommender.llm.dto.ModelGenerationRequest;
import com.evready.recommender.llm.dto.ModelGenerationResponse;
import com.evready.recommender.llm.dto.OllamaGenerateRequest;
import com.evready.recommender.llm.dto.OllamaGenerateResponse;
import com.evready.recommender.llm.service.ModelGenerationClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Locale;

@Component
public class OllamaModelGenerationClient implements ModelGenerationClient {

    private static final String PROVIDER = "OLLAMA";

    private final RestClient restClient;
    private final LlmProperties properties;

    public OllamaModelGenerationClient(RestClient.Builder restClientBuilder, LlmProperties properties) {
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.ollama().baseUrl())
                .build();
    }

    @Override
    public ModelGenerationResponse generate(ModelGenerationRequest request) {
        requireOllamaProvider();

        OllamaGenerateResponse response = restClient.post()
                .uri("/api/generate")
                .body(toOllamaRequest(request))
                .retrieve()
                .body(OllamaGenerateResponse.class);

        if (response == null || response.response() == null || response.response().isBlank()) {
            throw new IllegalStateException("Ollama returned an empty response.");
        }

        return new ModelGenerationResponse(
                response.response(),
                PROVIDER,
                properties.ollama().model(),
                properties.ollama().temperature(),
                runConfigJson()
        );
    }

    private OllamaGenerateRequest toOllamaRequest(ModelGenerationRequest request) {
        return new OllamaGenerateRequest(
                properties.ollama().model(),
                request.prompt(),
                false,
                false,
                "json",
                new OllamaGenerateRequest.Options(
                        toDouble(properties.ollama().temperature()),
                        properties.ollama().numPredict(),
                        properties.ollama().numCtx()
                )
        );
    }

    private void requireOllamaProvider() {
        String provider = properties.provider();

        if (provider == null || !PROVIDER.equals(provider.trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalStateException("Unsupported LLM provider: " + provider);
        }
    }

    private Double toDouble(BigDecimal value) {
        if (value == null) {
            return 0.0;
        }

        return value.doubleValue();
    }

    private String runConfigJson() {
        return """
                {"numPredict":%d,"numCtx":%d}
                """.formatted(
                properties.ollama().numPredict(),
                properties.ollama().numCtx()
        ).trim();
    }
}