package com.example.retail.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * Manually initializes the database using schema.sql and data.sql
 * when spring.sql.init.* is disabled or overridden.
 */
@Configuration
public class DBInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DBInitializer.class);

    private final DataSource dataSource;


    public DBInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initializeDatabase() {

        logger.info("Initializing schema and data from schema.sql and data.sql...");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema.sql"));
        populator.addScript(new ClassPathResource("data.sql"));
        populator.execute(dataSource);

        logger.info("Manual SQL initialization completed.");
    }
}
