package com.shipwise.payment;

import com.shipwise.common.BusinessException;

public class PaymentFailedException extends BusinessException {

    private final long paymentId;

    public PaymentFailedException(long paymentId, String message) {
        super(message);
        this.paymentId = paymentId;
    }

    public long getPaymentId() {
        return paymentId;
    }
}