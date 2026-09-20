package com.shipwise.order.saga;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.util.List;

@ActivityInterface
public interface OrderSagaActivities {

    @ActivityMethod
    List<ItemLine> loadLines(long orderId);

    @ActivityMethod
    void reserveStock(long productId, int quantity);

    @ActivityMethod
    void releaseStock(long productId, int quantity);

    @ActivityMethod
    PaymentResult chargePayment(long orderId);

    @ActivityMethod
    void refundPayment(long paymentId);

    @ActivityMethod
    void createShipment(long orderId, String customerAddress);

    @ActivityMethod
    void setOrderStatus(long orderId, String status, String failureReason);

    @ActivityMethod
    void confirmOrder(long orderId);
}