package com.shipwise.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        Long id,
        String customerName,
        String customerEmail,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items
) {
    public BigDecimal total() {
        return items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}