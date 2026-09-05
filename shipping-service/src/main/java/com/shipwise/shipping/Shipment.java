package com.shipwise.shipping;

import java.time.Instant;

public record Shipment(
        Long id,
        Long orderId,
        String address,
        String carrier,
        String trackingNumber,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}