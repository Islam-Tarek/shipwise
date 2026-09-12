package com.shipwise.order;

import java.time.Duration;
import java.util.List;

import com.shipwise.common.NotFoundException;
import com.shipwise.order.saga.OrderOutcome;
import com.shipwise.order.saga.OrderSagaWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private static final Duration SAGA_WAIT_TIMEOUT = Duration.ofSeconds(60);

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final Jdbi jdbi;
    private final WorkflowClient workflowClient;

    OrderService(Jdbi jdbi, WorkflowClient workflowClient) {
        this.jdbi = jdbi;
        this.workflowClient = workflowClient;
    }

    public Order placeOrder(long userId, CreateOrderRequest request) {
        long orderId = createOrder(userId, request);
        startOrderSaga(orderId, request.address());
        return getOrder(orderId);
    }

    private long createOrder(long userId, CreateOrderRequest request) {
        return jdbi.inTransaction(handle -> {
            OrderDao orderDao = handle.attach(OrderDao.class);
            OrderItemDao itemDao = handle.attach(OrderItemDao.class);
            long orderId = orderDao.create(userId,
                    request.customerName().trim(),
                    request.customerEmail().trim().toLowerCase());
            for (CreateOrderRequest.ItemLine item : request.items()) {
                itemDao.insert(orderId, item.productId(), item.productName(), item.unitPrice(), item.quantity());
            }
            log.info("order {} created with {} line(s)", orderId, request.items().size());
            return orderId;
        });
    }

    private void startOrderSaga(long orderId, String customerAddress) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setWorkflowId("order-" + orderId)
                .setTaskQueue(OrderSagaWorkflow.TASK_QUEUE)
                .setWorkflowExecutionTimeout(Duration.ofMinutes(5))
                .setWorkflowRunTimeout(Duration.ofMinutes(2))
                .build();
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(OrderSagaWorkflow.WORKFLOW_TYPE, options);
        try {
            stub.start(orderId, customerAddress == null ? "" : customerAddress);
            OrderOutcome outcome = stub.getResult(
                    SAGA_WAIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS, OrderOutcome.class);
            log.info("order {} saga completed: {}", orderId, outcome);
        } catch (TimeoutException e) {
            log.warn("order {} saga did not finish within {}s, order left as-is",
                    orderId, SAGA_WAIT_TIMEOUT.toSeconds());
        } catch (Exception e) {
            log.warn("order {} saga failed or timed out, order left as-is: {}", orderId, e.getMessage());
        }
    }

    public Order getOrder(long id) {
        OrderRow row = jdbi.withExtension(OrderDao.class, dao ->
                dao.findById(id)
                        .orElseThrow(() -> new NotFoundException("order %d not found".formatted(id))));
        return toOrder(row);
    }

    public List<Order> listOrdersForUser(long userId) {
        List<OrderRow> rows = jdbi.withExtension(OrderDao.class, dao -> dao.findByUserId(userId));
        return rows.stream().map(this::toOrder).toList();
    }

    private Order toOrder(OrderRow row) {
        List<OrderItem> items = jdbi.withExtension(OrderItemDao.class, itemDao -> itemDao.findByOrderId(row.id()));
        return row.toOrder(items);
    }
}