package com.shipwise.inventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class StockSyncService {

    private static final Logger log = LoggerFactory.getLogger(StockSyncService.class);

    private final RestClient productClient;

    StockSyncService(@Qualifier("productClient") RestClient productClient) {
        this.productClient = productClient;
    }

    public void push(long productId, int quantity) {
        try {
            productClient.put()
                    .uri("/api/products/by-external/{externalId}/stock?quantity={qty}", productId, quantity)
                    .retrieve()
                    .toBodilessEntity();
            log.info("synced product {} stock_quantity -> {}", productId, quantity);
        } catch (Exception e) {
            log.warn("stock sync for product {} failed: {}", productId, e.getMessage());
        }
    }
}