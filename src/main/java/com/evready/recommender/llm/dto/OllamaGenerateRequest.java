package com.evready.recommender.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OllamaGenerateRequest(
        String model,
        String prompt,
        Boolean stream,
        Boolean think,
        String format,
        Options options
) {

    public record Options(
            Double temperature,

            @JsonProperty("num_predict")
            Integer numPredict,

            @JsonProperty("num_ctx")
            Integer numCtx
    ) {
    }
}