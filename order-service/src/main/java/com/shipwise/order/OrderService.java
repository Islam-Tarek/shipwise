package com.shipwise.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.shipwise.common.NotFoundException;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final Jdbi jdbi;
    private final RestClient inventoryClient;
    private final RestClient paymentClient;
    private final RestClient shippingClient;

    OrderService(Jdbi jdbi,
                 @Qualifier("inventoryClient") RestClient inventoryClient,
                 @Qualifier("paymentClient") RestClient paymentClient,
                 @Qualifier("shippingClient") RestClient shippingClient) {
        this.jdbi = jdbi;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
        this.shippingClient = shippingClient;
    }

    @Transactional
    public Order placeOrder(long userId, CreateOrderRequest request) {
        List<Reservation> reserved = new ArrayList<>();
        Long paymentId = null;

        long orderId = jdbi.withExtension(OrderDao.class, dao ->
                dao.create(userId, request.customerName().trim(), request.customerEmail().trim().toLowerCase()));

        jdbi.useExtension(OrderItemDao.class, dao -> {
            for (CreateOrderRequest.ItemLine item : request.items()) {
                dao.insert(orderId, item.productId(), item.productName(), item.unitPrice(), item.quantity());
            }
        });
        log.info("order {} created with {} line(s)", orderId, request.items().size());

        try {
            for (CreateOrderRequest.ItemLine item : request.items()) {
                reserveStock(item.productId(), item.quantity());
                reserved.add(new Reservation(item.productId(), item.quantity()));
            }

            BigDecimal total = orderTotal(orderId);
            paymentId = chargePayment(orderId, total);

            createShipment(orderId, request);

            markStatus(orderId, "CONFIRMED", null);
            log.info("order {} CONFIRMED (payment {})", orderId, paymentId);
        } catch (Exception e) {
            log.warn("order {} failed at saga step: {}", orderId, e.getMessage());
            compensate(orderId, reserved, paymentId);
        }

        return getOrder(orderId);
    }

    private void compensate(long orderId, List<Reservation> reserved, Long paymentId) {
        if (paymentId != null) {
            try {
                paymentClient.post().uri("/api/payments/{id}/refund", paymentId).retrieve().toBodilessEntity();
                log.info("order {} refunded payment {}", orderId, paymentId);
            } catch (Exception e) {
                log.warn("order {} refund of payment {} failed: {}", orderId, paymentId, e.getMessage());
            }
        }
        for (Reservation reservation : reserved) {
            try {
                inventoryClient.post()
                        .uri("/api/inventory/{productId}/restock?quantity={qty}",
                                reservation.productId(), reservation.quantity())
                        .retrieve().toBodilessEntity();
                log.info("order {} released {} units of product {}", orderId,
                        reservation.quantity(), reservation.productId());
            } catch (Exception e) {
                log.warn("order {} restock of product {} failed: {}", orderId, reservation.productId(), e.getMessage());
            }
        }
        markStatus(orderId, "FAILED", "downstream step failed during saga");
    }

    record Reservation(long productId, int quantity) {}

    private void reserveStock(long productId, int quantity) {
        inventoryClient.post()
                .uri("/api/inventory/{productId}/reserve?quantity={qty}", productId, quantity)
                .retrieve()
                .toBodilessEntity();
    }

    private BigDecimal orderTotal(long orderId) {
        return jdbi.withExtension(OrderItemDao.class, dao ->
                dao.findByOrderId(orderId).stream()
                        .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private long chargePayment(long orderId, BigDecimal total) {
        Map<String, Object> body = paymentClient.post()
                .uri("/api/payments")
                .body(Map.of("orderId", orderId, "amount", total, "currency", "USD"))
                .retrieve()
                .body(Map.class);
        Object raw = body.get("id");
        return raw instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(raw));
    }

    private void createShipment(long orderId, CreateOrderRequest request) {
        String address = request.address() == null || request.address().isBlank()
                ? "no address provided" : request.address();
        shippingClient.post()
                .uri("/api/shipments")
                .body(Map.of("orderId", orderId, "address", address, "carrier", "DHL"))
                .retrieve()
                .toBodilessEntity();
        log.info("order {} shipment created", orderId);
    }

    private void markStatus(long orderId, String status, String failureReason) {
        jdbi.useExtension(OrderDao.class, dao -> dao.setStatus(orderId, status, failureReason));
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