package com.shipwise.product;

import java.math.BigDecimal;
import java.time.Instant;

public record Product(
        Long id,
        Long categoryId,
        String externalId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        int stockQuantity,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {
}
