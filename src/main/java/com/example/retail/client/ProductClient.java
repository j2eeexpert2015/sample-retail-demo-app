package com.example.retail.client;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Client to call external product service (WireMock).
 */
@Component
public class ProductClient {

    private static final Logger logger = LoggerFactory.getLogger(ProductClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private String baseUrl;

    @PostConstruct
    public void init() {
        // Fetch base URL from system property set by TestContainerConfig
        this.baseUrl = System.getProperty("app.external.product-service.url", "http://localhost:8081");
        logger.info("External product service base URL resolved as: {}", baseUrl);
    }

    public String fetchProductById(Long id) {
        logger.info("Calling external product service for ID: {}", id);
        try {
            return restTemplate.getForObject(baseUrl + "/api/products/" + id, String.class);
        } catch (Exception e) {
            logger.error("Failed to call external product API", e);
            return "External product service error";
        }
    }
}
