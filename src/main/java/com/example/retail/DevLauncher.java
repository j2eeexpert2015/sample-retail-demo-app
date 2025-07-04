package com.example.retail;

import com.example.retail.config.TestContainerConfig;
import org.springframework.boot.SpringApplication;

public class DevLauncher {

    public static void main(String[] args) {
        SpringApplication
                .from(SampleRetailDemoApp::main)
                .with(TestContainerConfig.class)
                .run(args);
    }
}

