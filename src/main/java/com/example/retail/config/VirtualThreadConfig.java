package com.example.retail.config;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;

@Configuration
public class VirtualThreadConfig {

    @ConditionalOnProperty(name = "spring.threads.virtual.enabled", havingValue = "true")
    @Bean
    public TomcatProtocolHandlerCustomizer<?> tomcatExecutorCustomizer() {
        return protocolHandler -> protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
    }
}
