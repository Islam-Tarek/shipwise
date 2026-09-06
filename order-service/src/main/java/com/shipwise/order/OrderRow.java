package com.shipwise.order;

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
        return new Order(id, userId, customerName, customerEmail, status, failureReason, createdAt, updatedAt, items);
    }
}