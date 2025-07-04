package com.example.retail.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
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

    private final RestTemplate restTemplate;
    private final RestClient restClient;
    private final String baseUrl;
    private final String httpClientType;

    public ProductClient(RestTemplate restTemplate,
                         RestClient restClient,
                         @Qualifier("productServiceBaseUrl") String baseUrl,
                         @Value("${app.http.client.type:resttemplate}") String httpClientType) {
        this.restTemplate = restTemplate;
        this.restClient = restClient;
        this.baseUrl = baseUrl;
        this.httpClientType = httpClientType;

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
