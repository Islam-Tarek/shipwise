package com.shipwise.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientsConfig {

    @Bean("inventoryClient")
    RestClient inventoryClient(@Value("${services.inventory}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("paymentClient")
    RestClient paymentClient(@Value("${services.payment}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }

    @Bean("shippingClient")
    RestClient shippingClient(@Value("${services.shipping}") String baseUrl) {
        return RestClient.builder().baseUrl(baseUrl).build();
    }
}