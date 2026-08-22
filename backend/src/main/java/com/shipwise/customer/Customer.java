package com.shipwise.customer;

import java.time.Instant;

public record Customer(
        Long id,
        String name,
        String email,
        String address,
        Instant createdAt,
        Instant updatedAt
) {
}
