package com.evready.recommender.llm.service;

import com.evready.recommender.llm.dto.ModelGenerationRequest;
import com.evready.recommender.llm.dto.ModelGenerationResponse;

public interface ModelGenerationClient {

    ModelGenerationResponse generate(ModelGenerationRequest request);
}