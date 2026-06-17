package com.evready.recommender.common.api.error;

public record FieldErrorResponse(
        String field,
        String message
) {
}