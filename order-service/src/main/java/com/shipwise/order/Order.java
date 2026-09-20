package com.shipwise.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Order(
        Long id,
        Long userId,
        String customerName,
        String customerEmail,
        String status,
        String failureReason,
        BigDecimal totalAmount,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItem> items
) {
}