package com.shipwise.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        OrderStatus status,
        BigDecimal totalAmount,
        List<Item> items,
        Instant createdAt
) {

    public record Item(Long productId, String productName, int quantity, BigDecimal unitPrice) {
    }
}
