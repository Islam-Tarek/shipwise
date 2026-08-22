package com.shipwise.order;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.shipwise.customer.CustomerRequest;

public record CheckoutRequest(
        @NotNull @Valid CustomerRequest customer,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
    public Set<Long> productIds() {
        return items.stream().map(OrderItemRequest::productId).collect(Collectors.toSet());
    }
}
