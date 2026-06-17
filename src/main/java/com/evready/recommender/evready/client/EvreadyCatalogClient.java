package com.evready.recommender.evready.client;

import com.evready.recommender.config.EvreadyApiProperties;
import com.evready.recommender.evready.dto.EvreadyPageResponse;
import com.evready.recommender.evready.dto.EvreadyVehicleResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class EvreadyCatalogClient {

    private static final int PAGE_SIZE = 50;

    private final RestClient restClient;

    public EvreadyCatalogClient(RestClient.Builder restClientBuilder, EvreadyApiProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    public List<EvreadyVehicleResponse> fetchAllVehicles() {
        List<EvreadyVehicleResponse> vehicles = new ArrayList<>();
        int page = 0;
        int totalPages = 1;

        while (page < totalPages) {
            EvreadyPageResponse<EvreadyVehicleResponse> response = fetchVehiclePage(page);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                break;
            }

            vehicles.addAll(response.content());
            totalPages = Math.max(response.totalPages(), 1);
            page++;
        }

        return vehicles;
    }

    private EvreadyPageResponse<EvreadyVehicleResponse> fetchVehiclePage(int page) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/vehicles")
                        .queryParam("page", page)
                        .queryParam("size", PAGE_SIZE)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }
}