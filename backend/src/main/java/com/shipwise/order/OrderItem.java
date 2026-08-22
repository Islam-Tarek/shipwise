package com.shipwise.order;

import java.math.BigDecimal;

public record OrderItem(
        Long id,
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal unitPrice
) {
}
