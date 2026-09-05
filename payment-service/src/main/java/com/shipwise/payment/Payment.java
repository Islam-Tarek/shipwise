package com.shipwise.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record Payment(
        Long id,
        Long orderId,
        BigDecimal amount,
        String currency,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean succeeded() {
        return "SUCCEEDED".equals(status);
    }
}