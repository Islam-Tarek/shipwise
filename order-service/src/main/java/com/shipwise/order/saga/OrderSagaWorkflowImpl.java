package com.shipwise.order.saga;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.List;

public class OrderSagaWorkflowImpl implements OrderSagaWorkflow {

    private static final String FAILED_REASON = "downstream step failed during saga";

    private final OrderSagaActivities activities = Workflow.newActivityStub(
            OrderSagaActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setMaximumAttempts(3)
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(1.5)
                            .setMaximumInterval(Duration.ofSeconds(2))
                            .build())
                    .build());

    @Override
    public OrderOutcome place(long orderId, String customerAddress) {
        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .setContinueWithError(true)
                .build());
        PaymentResult payment = null;
        try {
            List<ItemLine> lines = activities.loadLines(orderId);
            for (ItemLine line : lines) {
                activities.reserveStock(line.productId(), line.quantity());
                saga.addCompensation(activities::releaseStock, line.productId(), line.quantity());
            }

            payment = activities.chargePayment(orderId);
            saga.addCompensation(activities::refundPayment, payment.paymentId());

            activities.createShipment(orderId, customerAddress);
            activities.confirmOrder(orderId);
            return new OrderOutcome(orderId, "CONFIRMED", payment.paymentId());
        } catch (Exception e) {
            activities.setOrderStatus(orderId, "FAILED", FAILED_REASON);
            saga.compensate();
            return new OrderOutcome(orderId, "FAILED", payment == null ? null : payment.paymentId());
        }
    }
}