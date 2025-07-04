package com.example.retail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

@TestConfiguration
public class TestContainerConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestContainerConfig.class);

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean isVirtualThreadEnabled;

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

    @Bean
    public WireMockContainer wireMockContainer() {
        WireMockContainer wiremock = new WireMockContainer("wiremock/wiremock:3.3.1")
                .withClasspathResourceMapping("mappings", "/home/wiremock/mappings", BindMode.READ_ONLY)
                .withClasspathResourceMapping("__files", "/home/wiremock/__files", BindMode.READ_ONLY);
        logger.info("Starting WireMock container...");
        wiremock.start();
        String baseUrl = wiremock.getBaseUrl();
        System.setProperty("app.external.product-service.url", baseUrl);
        logger.info("WireMock running at: {}", baseUrl);
        return wiremock;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RestClient restClient(WireMockContainer wireMockContainer, @Value("${spring.threads.virtual.enabled:false}") boolean isVirtualThreadEnabled) {
        String baseUrl = wireMockContainer.getBaseUrl();

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1); // avoid HTTP/2 issues

        if (isVirtualThreadEnabled) {
            clientBuilder.executor(Executors.newVirtualThreadPerTaskExecutor());
        }

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new JdkClientHttpRequestFactory(clientBuilder.build()))
                .build();
    }


    @Bean
    public String productServiceBaseUrl(WireMockContainer wireMockContainer) {
        String baseUrl = wireMockContainer.getBaseUrl();
        logger.info("Exposing product service base URL: {}", baseUrl);
        return baseUrl;
    }

}
