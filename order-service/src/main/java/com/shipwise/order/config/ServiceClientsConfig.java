package com.shipwise.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ServiceClientsConfig {

    @Bean("inventoryClient")
    RestClient inventoryClient(RestClient.Builder builder, @Value("${services.inventory}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean("paymentClient")
    RestClient paymentClient(RestClient.Builder builder, @Value("${services.payment}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }

    @Bean("shippingClient")
    RestClient shippingClient(RestClient.Builder builder, @Value("${services.shipping}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}