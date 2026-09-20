package com.shipwise.common.event;

import java.math.BigDecimal;
import java.util.List;
import java.time.Instant;

public record OrderConfirmedEvent(
        long orderId,
        String customerName,
        String customerEmail,
        BigDecimal total,
        Instant createdAt,
        List<OrderConfirmedItem> items
) {
}