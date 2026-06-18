package com.evready.recommender.llm.service.impl;

import com.evready.recommender.llm.config.LlmProperties;
import com.evready.recommender.llm.dto.ModelGenerationRequest;
import com.evready.recommender.llm.dto.ModelGenerationResponse;
import com.evready.recommender.llm.dto.OllamaGenerateRequest;
import com.evready.recommender.llm.dto.OllamaGenerateResponse;
import com.evready.recommender.llm.service.ModelGenerationClient;
import com.evready.recommender.llm.service.ModelGenerationTimeoutException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.Locale;

@Component
public class OllamaModelGenerationClient implements ModelGenerationClient {

    private static final String PROVIDER = "OLLAMA";

    private final RestClient restClient;
    private final LlmProperties properties;

    public OllamaModelGenerationClient(LlmProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.ollama().baseUrl())
                .requestFactory(ollamaRequestFactory())
                .build();
    }

    @Override
    public ModelGenerationResponse generate(ModelGenerationRequest request) {
        requireOllamaProvider();

        OllamaGenerateResponse response;

        try {
            response = restClient.post()
                    .uri("/api/generate")
                    .body(toOllamaRequest(request))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);
        } catch (RestClientException ex) {
            if (isLikelyTimeout(ex)) {
                throw new ModelGenerationTimeoutException(
                        "Ollama model generation timed out after " + properties.ollama().readTimeout() + ".",
                        ex
                );
            }

            throw ex;
        }

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

    private SimpleClientHttpRequestFactory ollamaRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(toMillis(properties.ollama().connectTimeout(), "Ollama connect timeout"));
        factory.setReadTimeout(toMillis(properties.ollama().readTimeout(), "Ollama read timeout"));
        return factory;
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

    private int toMillis(Duration duration, String propertyName) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalStateException(propertyName + " must be greater than zero.");
        }

        long millis = duration.toMillis();

        if (millis > Integer.MAX_VALUE) {
            throw new IllegalStateException(propertyName + " is too large.");
        }

        return Math.toIntExact(millis);
    }

    private boolean isLikelyTimeout(Throwable throwable) {
        if (hasCause(throwable, SocketTimeoutException.class)) {
            return true;
        }

        String message = throwable.getMessage();

        if (message == null) {
            return false;
        }

        String normalizedMessage = message.toLowerCase(Locale.ROOT);

        return normalizedMessage.contains("timed out")
                || normalizedMessage.contains("read timed out")
                || normalizedMessage.contains("error while extracting response");
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;

        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private String runConfigJson() {
        return """
                {"numPredict":%d,"numCtx":%d,"connectTimeout":"%s","readTimeout":"%s"}
                """.formatted(
                properties.ollama().numPredict(),
                properties.ollama().numCtx(),
                properties.ollama().connectTimeout(),
                properties.ollama().readTimeout()
        ).trim();
    }
}