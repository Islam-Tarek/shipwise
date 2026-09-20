package com.shipwise.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderRow(
        Long id,
        Long userId,
        String customerName,
        String customerEmail,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    Order toOrder(java.util.List<OrderItem> items) {
        BigDecimal total = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(id, userId, customerName, customerEmail, status, failureReason, total, createdAt, updatedAt, items);
    }
}