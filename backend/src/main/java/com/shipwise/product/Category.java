package com.shipwise.product;

import java.time.Instant;

public record Category(
        Long id,
        String name,
        String slug,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
