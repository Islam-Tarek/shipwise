package com.shipwise.order.saga;

import com.shipwise.order.OrderDao;
import com.shipwise.order.OrderItemDao;
import io.temporal.failure.ApplicationFailure;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class OrderSagaActivitiesImpl implements OrderSagaActivities {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaActivitiesImpl.class);

    private final Jdbi jdbi;
    private final RestClient inventoryClient;
    private final RestClient paymentClient;
    private final RestClient shippingClient;

    OrderSagaActivitiesImpl(Jdbi jdbi,
                            @Qualifier("inventoryClient") RestClient inventoryClient,
                            @Qualifier("paymentClient") RestClient paymentClient,
                            @Qualifier("shippingClient") RestClient shippingClient) {
        this.jdbi = jdbi;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.shippingClient = shippingClient;
    }

    @Override
    public List<ItemLine> loadLines(long orderId) {
        return jdbi.withExtension(OrderItemDao.class, dao -> dao.findByOrderId(orderId).stream()
                .map(item -> new ItemLine(item.productId(), item.quantity()))
                .toList());
    }

    @Override
    public void reserveStock(long productId, int quantity) {
        try {
            inventoryClient.post()
                    .uri("/api/inventory/{productId}/reserve?quantity={qty}", productId, quantity)
                    .retrieve()
                    .toBodilessEntity();
            log.info("temporal saga: reserved {} units of product {} for order activity", quantity, productId);
        } catch (RestClientResponseException e) {
            throw permanent("INSUFFICIENT_STOCK",
                    "reserve failed for product " + productId + ": " + e.getMessage());
        }
    }

    @Override
    public void releaseStock(long productId, int quantity) {
        inventoryClient.post()
                .uri("/api/inventory/{productId}/restock?quantity={qty}", productId, quantity)
                .retrieve()
                .toBodilessEntity();
        log.info("temporal saga: released {} units of product {} (compensation)", quantity, productId);
    }

    @Override
    public PaymentResult chargePayment(long orderId) {
        BigDecimal total = orderTotal(orderId);
        Map<String, Object> body;
        try {
            body = paymentClient.post()
                    .uri("/api/payments")
                    .body(Map.of("orderId", orderId, "amount", total, "currency", "USD"))
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            throw permanent("PAYMENT_DECLINED", "charge failed for order " + orderId + ": " + e.getMessage());
        }
        Object raw = body.get("id");
        long paymentId = raw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(raw));
        log.info("temporal saga: charged payment {} for order {}", paymentId, orderId);
        return new PaymentResult(paymentId);
    }

    @Override
    public void refundPayment(long paymentId) {
        paymentClient.post()
                .uri("/api/payments/{id}/refund", paymentId)
                .retrieve()
                .toBodilessEntity();
        log.info("temporal saga: refunded payment {} (compensation)", paymentId);
    }

    @Override
    public void createShipment(long orderId, String customerAddress) {
        String address = customerAddress == null || customerAddress.isBlank()
                ? "no address provided" : customerAddress;
        shippingClient.post()
                .uri("/api/shipments")
                .body(Map.of("orderId", orderId, "address", address, "carrier", "DHL"))
                .retrieve()
                .toBodilessEntity();
        log.info("temporal saga: shipment created for order {}", orderId);
    }

    @Override
    public void setOrderStatus(long orderId, String status, String failureReason) {
        jdbi.useExtension(OrderDao.class, dao -> dao.setStatus(orderId, status, failureReason));
        log.info("temporal saga: order {} status -> {}", orderId, status);
    }

    private BigDecimal orderTotal(long orderId) {
        return jdbi.withExtension(OrderItemDao.class, dao -> dao.findByOrderId(orderId).stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private ApplicationFailure permanent(String type, String message) {
        return ApplicationFailure.newNonRetryableFailure(type, message);
    }
}