package com.shipwise.shipping;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateShipmentRequest(
        @NotNull Long orderId,
        @NotBlank String address,
        @NotBlank String carrier
) {
}