package com.example.retail.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.wiremock.integrations.testcontainers.WireMockContainer;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.model.ToxicDirection;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.Executors;

@TestConfiguration
public class TestContainerConfig {

    private static final Logger logger = LoggerFactory.getLogger(TestContainerConfig.class);

    // Database latency configuration
    private static final long DATABASE_LATENCY_MS = 500; 

    private static final Network network = Network.newNetwork();

    private static Proxy postgresqlProxy;

    @Value("${spring.threads.virtual.enabled:false}")
    private boolean isVirtualThreadEnabled;

    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
                .withDatabaseName("retaildb")
                .withUsername("retail_user")
                .withPassword("retail_pass")
                .withNetwork(network)
                .withNetworkAliases("postgres");
        logger.info("Starting PostgreSQL container...");
        postgres.start(); // Explicitly start the container
        return postgres;
    }

    @Bean
    public ToxiproxyContainer toxiproxyContainer() {
        ToxiproxyContainer toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
                .withNetwork(network);
        logger.info("Starting Toxiproxy container...");
        toxiproxy.start(); // Explicitly start the container
        return toxiproxy;
    }

    @Bean
    @DependsOn({"postgresContainer", "toxiproxyContainer"})
    public DataSource dataSource(PostgreSQLContainer<?> postgres, ToxiproxyContainer toxiproxy) throws IOException {
        // Create Toxiproxy client and proxy
        ToxiproxyClient toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
        postgresqlProxy = toxiproxyClient.createProxy("postgresql", "0.0.0.0:8666", "postgres:5432");

        // Don't add latency initially - let the application start up first
        // Latency will be added after application context is ready
        logger.info("PostgreSQL proxy created - latency will be added after startup");

        // Configure DataSource to use the proxy
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
                toxiproxy.getHost(),
                toxiproxy.getMappedPort(8666),
                postgres.getDatabaseName());

        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(postgres.getUsername());
        dataSource.setPassword(postgres.getPassword());
        dataSource.setDriverClassName("org.postgresql.Driver");

        // Standard configuration for development
        dataSource.setConnectionTimeout(30000); // 30 seconds
        dataSource.setMaximumPoolSize(5);
        dataSource.setMinimumIdle(1);

        logger.info("PostgreSQL DataSource configured through Toxiproxy (latency can be added later)");
        logger.info("JDBC URL: {}", jdbcUrl);

        return dataSource;
    }

    @Bean
    public LatencyController latencyController() {
        return new LatencyController();
    }

    @Bean
    public ApplicationStartupLatencyInjector applicationStartupLatencyInjector(LatencyController latencyController) {
        return new ApplicationStartupLatencyInjector(latencyController);
    }

    // Component that adds latency after application startup
    public static class ApplicationStartupLatencyInjector implements ApplicationListener<ApplicationReadyEvent> {

        private final LatencyController latencyController;

        public ApplicationStartupLatencyInjector(LatencyController latencyController) {
            this.latencyController = latencyController;
        }

        @Override
        public void onApplicationEvent(ApplicationReadyEvent event) {
            try {
                // Add latency after application is fully ready
                if (!latencyController.isLatencyActive()) {
                    latencyController.addConfiguredLatency();
                    logger.info("🐌 Application startup complete - {}ms database latency now active!", DATABASE_LATENCY_MS);
                } else {
                    logger.info("🐌 Database latency already active - skipping");
                }
            } catch (Exception e) {
                logger.error("Failed to add database latency after startup", e);
            }
        }
    }

    // Helper class to control latency after application startup
    public static class LatencyController {

        private boolean latencyActive = false;

        public void addLatency(long milliseconds) throws IOException {
            if (postgresqlProxy != null) {
                // Remove any existing latency first to avoid duplicates
                removeLatency();

                postgresqlProxy.toxics().latency("postgresql-latency", ToxicDirection.DOWNSTREAM, milliseconds);
                latencyActive = true;
                logger.info("Added {}ms latency to PostgreSQL connection", milliseconds);
            }
        }

        public void removeLatency() throws IOException {
            if (postgresqlProxy != null && latencyActive) {
                try {
                    postgresqlProxy.toxics().get("postgresql-latency").remove();
                    latencyActive = false;
                    logger.info("Removed latency from PostgreSQL connection");
                } catch (Exception e) {
                    logger.warn("Could not remove latency (may not exist): {}", e.getMessage());
                    latencyActive = false;
                }
            }
        }

        public void add3SecondLatency() throws IOException {
            addLatency(3000);
        }

        public void addConfiguredLatency() throws IOException {
            addLatency(DATABASE_LATENCY_MS);
        }

        public void add1SecondLatency() throws IOException {
            addLatency(1000);
        }

        public boolean isLatencyActive() {
            return latencyActive;
        }

        public void listActiveToxics() throws IOException {
            if (postgresqlProxy != null) {
                try {
                    var toxics = postgresqlProxy.toxics().getAll();
                    logger.info("Active toxics count: {}", toxics.size());
                    for (var toxic : toxics) {
                        logger.info("  - Toxic name: {}", toxic.getName());
                    }
                } catch (Exception e) {
                    logger.warn("Could not list toxics: {}", e.getMessage());
                }
            }
        }
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

    // Utility method to access the proxy for dynamic manipulation during tests
    public static Proxy getPostgresqlProxy() {
        return postgresqlProxy;
    }
}