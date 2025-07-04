package com.example.retail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

@TestConfiguration
public class TestContainerConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestContainerConfig.class);

    /**
     * Initializes a PostgreSQL container for local dev/testing using Testcontainers.
     */
    @Bean
    @ServiceConnection
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("retaildb")
                .withUsername("retail_user")
                .withPassword("retail_pass");
        logger.info("Starting PostgreSQL container...");
        return postgres;
    }

    /**
     * Initializes a WireMock container and maps stubs from classpath.
     * Sets the base URL as a system property to be used by the application.
     */
    @Bean
    public WireMockContainer wireMockContainer() {
        WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock:3.3.1")
                .withClasspathResourceMapping("mappings", "/home/wiremock/mappings", BindMode.READ_ONLY)
                .withClasspathResourceMapping("__files", "/home/wiremock/__files", BindMode.READ_ONLY);
        logger.info("Starting WireMock container...");
        wiremock.start();
        System.setProperty("app.external.product-service.url", wiremock.getBaseUrl());
        logger.info("WireMock running at: {}", wiremock.getBaseUrl());
        return wiremock;
    }
}
