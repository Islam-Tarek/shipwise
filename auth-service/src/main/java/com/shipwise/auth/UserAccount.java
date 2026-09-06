package com.shipwise.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;

public record UserAccount(
        Long id,
        String name,
        String email,
        @JsonIgnore String passwordHash,
        String address,
        Instant createdAt,
        Instant updatedAt
) {
}
