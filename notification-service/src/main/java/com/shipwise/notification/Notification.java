package com.shipwise.notification;

import java.time.Instant;

public record Notification(
        Long id,
        String type,
        String recipient,
        String message,
        String channel,
        Instant createdAt
) {
}