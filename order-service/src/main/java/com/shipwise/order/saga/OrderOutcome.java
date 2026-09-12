package com.shipwise.order.saga;

public record OrderOutcome(long orderId, String status, Long paymentId) {
}