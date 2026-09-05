package com.shipwise.order;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record CreateOrderRequest(
        @NotBlank String customerName,
        @NotBlank String customerEmail,
        String address,
        @NotEmpty @Valid List<ItemLine> items
) {
    public record ItemLine(
            long productId,
            int quantity,
            @NotBlank String productName,
            BigDecimal unitPrice,
            String imageUrl
    ) {}
}