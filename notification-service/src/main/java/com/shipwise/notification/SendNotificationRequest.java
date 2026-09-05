package com.shipwise.notification;

import jakarta.validation.constraints.NotBlank;

public record SendNotificationRequest(
        String type,
        @NotBlank String recipient,
        @NotBlank String message,
        @NotBlank String channel
) {
}