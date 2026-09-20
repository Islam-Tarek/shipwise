package com.shipwise.order.outbox;

import java.time.Instant;

public record OrderOutbox(
        long id,
        long orderId,
        String eventType,
        String payload,
        Instant createdAt,
        Instant sentAt,
        int attempts
) {
}