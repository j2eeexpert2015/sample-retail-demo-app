package com.example.retail.client;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

/**
 * Client to call external product service (WireMock).
 * Supports both RestTemplate and RestClient based on configuration.
 */
@Component
public class ProductClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductClient.class);

    @Value("${app.http.client.type:resttemplate}")
    private String httpClientType;

    private final RestTemplate restTemplate;
    private final RestClient restClient;
    private String baseUrl;

    public ProductClient(RestTemplate restTemplate, RestClient restClient) {
        this.restTemplate = restTemplate;
        this.restClient = restClient;
    }

    @PostConstruct
    public void init() {
        // Fetch base URL from system property set by TestContainerConfig
        this.baseUrl = System.getProperty("app.external.product-service.url", "http://localhost:8081");
        logger.info("External product service base URL resolved as: {}", baseUrl);
        logger.info("Using HTTP client type: {}", httpClientType);
    }

    public String fetchProductById(Long id) {
        logger.info("Calling external product service using {} for ID: {}", httpClientType, id);

        try {
            if ("restclient".equalsIgnoreCase(httpClientType)) {
                return restClient.get()
                        .uri(baseUrl + "/api/products/" + id)
                        .retrieve()
                        .body(String.class);
            } else {
                return restTemplate.getForObject(baseUrl + "/api/products/" + id, String.class);
            }
        } catch (Exception e) {
            logger.error("Failed to call external product API using {}", httpClientType, e);
            return "External product service error";
        }
    }
}