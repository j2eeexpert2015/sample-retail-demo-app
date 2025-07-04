package com.example.retail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot application.
 */
@SpringBootApplication
public class SampleRetailDemoApp {

    private static final Logger logger = LoggerFactory.getLogger(SampleRetailDemoApp.class);

    public static void main(String[] args) {
        logger.info("Starting SampleRetailDemoApp...");
        SpringApplication.run(SampleRetailDemoApp.class, args);
        logger.info("SampleRetailDemoApp started successfully.");
    }
}
