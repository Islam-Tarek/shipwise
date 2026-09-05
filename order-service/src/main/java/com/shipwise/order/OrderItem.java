package com.shipwise.order;

import java.math.BigDecimal;

public record OrderItem(
        Long id,
        Long orderId,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
}