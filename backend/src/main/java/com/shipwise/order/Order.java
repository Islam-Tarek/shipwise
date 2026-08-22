package com.shipwise.order;

import java.math.BigDecimal;
import java.time.Instant;

public record Order(
        Long id,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt
) {
}
