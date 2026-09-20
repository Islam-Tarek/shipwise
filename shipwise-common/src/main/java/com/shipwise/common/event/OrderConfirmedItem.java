package com.shipwise.common.event;

import java.math.BigDecimal;

public record OrderConfirmedItem(
        long productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
}