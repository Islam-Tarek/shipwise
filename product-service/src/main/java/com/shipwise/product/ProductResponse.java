package com.shipwise.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        int stockQuantity,
        String categorySlug
) {
    static ProductResponse from(Product product, String categorySlug) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.imageUrl(),
                product.stockQuantity(),
                categorySlug
        );
    }
}