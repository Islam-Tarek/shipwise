package com.shipwise.seed;

import java.math.BigDecimal;

record DummyJsonProduct(
        long id,
        String title,
        String description,
        String category,
        BigDecimal price,
        int stock,
        String thumbnail
) {
}