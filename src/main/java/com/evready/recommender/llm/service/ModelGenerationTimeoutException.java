package com.evready.recommender.llm.service;

public class ModelGenerationTimeoutException extends RuntimeException {

    public ModelGenerationTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}