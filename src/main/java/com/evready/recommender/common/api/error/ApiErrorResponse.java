package com.evready.recommender.common.api.error;

import java.util.List;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        List<FieldErrorResponse> fieldErrors
) {

    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(status, error, message, List.of());
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            String error,
            String message,
            List<FieldErrorResponse> fieldErrors
    ) {
        return new ApiErrorResponse(status, error, message, fieldErrors);
    }
}