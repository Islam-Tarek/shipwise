package com.shipwise.inventory.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientsConfig {

    @Bean("productClient")
    RestClient productClient(@Value("${services.product}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}