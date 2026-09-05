package com.shipwise.inventory;

import java.time.Instant;

public record Stock(
        Long id,
        Long productId,
        int quantity,
        Instant createdAt,
        Instant updatedAt
) {
}