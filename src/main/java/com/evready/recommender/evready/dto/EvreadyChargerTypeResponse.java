package com.evready.recommender.evready.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvreadyChargerTypeResponse(
        Long id,
        String code,
        String name
) {
}