package com.shipwise.order.saga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrderSagaWorkflow {

    String TASK_QUEUE = "ORDER_SAGA";
    String WORKFLOW_TYPE = "OrderSagaWorkflow";

    @WorkflowMethod
    OrderOutcome place(long orderId, String customerAddress);
}