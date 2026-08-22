package com.shipwise.order;

import java.math.BigDecimal;
import java.util.List;

public record OrderItemRequest(
        Long productId,
        Integer quantity
) {
}
