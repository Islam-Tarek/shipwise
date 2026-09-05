package com.shipwise.auth;

import java.time.Instant;

public record UserAccount(
        Long id,
        String name,
        String email,
        String passwordHash,
        String address,
        Instant createdAt,
        Instant updatedAt
) {
}
