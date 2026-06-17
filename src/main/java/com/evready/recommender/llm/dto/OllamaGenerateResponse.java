package com.evready.recommender.llm.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaGenerateResponse(
        String response,
        Boolean done
) {
}