package com.shipwise.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.shipwise.common.BusinessException;
import com.shipwise.common.NotFoundException;
import com.shipwise.customer.Customer;
import com.shipwise.customer.CustomerDao;
import com.shipwise.customer.CustomerRequest;
import com.shipwise.product.Product;
import com.shipwise.product.ProductDao;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final Jdbi jdbi;

    OrderService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    @Transactional
    public OrderResponse checkout(CheckoutRequest request) {
        Customer customer = resolveCustomer(request.customer());

        return jdbi.withExtension(OrderDao.class, orderDao -> {
            Map<Long, Product> products = loadProducts(request.productIds());

            BigDecimal total = BigDecimal.ZERO;
            for (OrderItemRequest item : request.items()) {
                Product product = validateProduct(products, item);
                total = total.add(product.price().multiply(BigDecimal.valueOf(item.quantity())));
            }

            long orderId = orderDao.insert(customer.id(), OrderStatus.PENDING, total);

            for (OrderItemRequest item : request.items()) {
                Product product = products.get(item.productId());
                orderDao.insertItem(new OrderItem(null, orderId, product.id(),
                        item.quantity(), product.price()));

                int updated = jdbi.withExtension(ProductDao.class, dao ->
                        dao.decrementStock(product.id(), item.quantity()));
                if (updated == 0) {
                    throw new BusinessException("insufficient stock for '%s'".formatted(product.name()));
                }
            }

            orderDao.updateStatus(orderId, OrderStatus.CONFIRMED);
            return buildResponse(orderId);
        });
    }

    public OrderResponse get(long id) {
        return jdbi.withExtension(OrderDao.class, dao -> {
            if (dao.findById(id).isEmpty()) {
                throw new NotFoundException("order %d not found".formatted(id));
            }
            return buildResponse(id);
        });
    }

    public List<OrderResponse> listByCustomer(long customerId) {
        return jdbi.withExtension(OrderDao.class, dao ->
                dao.findByCustomer(customerId).stream()
                        .map(order -> buildResponse(order.id()))
                        .toList()
        );
    }

    @Transactional
    public OrderResponse cancel(long id) {
        return jdbi.withExtension(OrderDao.class, dao -> {
            Order order = dao.findById(id)
                    .orElseThrow(() -> new NotFoundException("order %d not found".formatted(id)));
            if (order.status() != OrderStatus.PENDING) {
                throw new BusinessException(
                        "only pending orders can be cancelled, order %d is %s".formatted(id, order.status()));
            }
            for (OrderItem item : dao.findItems(id)) {
                jdbi.withExtension(ProductDao.class, pdao ->
                        pdao.restoreStock(item.productId(), item.quantity()));
            }
            dao.updateStatus(id, OrderStatus.CANCELLED);
            return buildResponse(id);
        });
    }

    private Customer resolveCustomer(CustomerRequest request) {
        String email = request.email().trim().toLowerCase();
        return jdbi.withExtension(CustomerDao.class, dao ->
                dao.findByEmail(email).orElseGet(() -> {
                    long id = dao.insert(request.name().trim(), email, request.address());
                    return dao.findById(id).orElseThrow();
                })
        );
    }

    private Map<Long, Product> loadProducts(java.util.Set<Long> ids) {
        return jdbi.withExtension(ProductDao.class, dao ->
                ids.stream()
                        .map(dao::findById)
                        .flatMap(java.util.Optional::stream)
                        .collect(Collectors.toMap(Product::id, Function.identity()))
        );
    }

    private Product validateProduct(Map<Long, Product> products, OrderItemRequest item) {
        if (item.quantity() == null || item.quantity() <= 0) {
            throw new BusinessException("quantity must be at least 1");
        }
        Product product = products.get(item.productId());
        if (product == null || !product.isActive()) {
            throw new BusinessException("unknown or unavailable product %s".formatted(item.productId()));
        }
        return product;
    }

    private OrderResponse buildResponse(long orderId) {
        return jdbi.withExtension(OrderDao.class, dao -> {
            Order order = dao.findById(orderId).orElseThrow();
            Customer customer = jdbi.withExtension(CustomerDao.class, cdao ->
                    cdao.findById(order.customerId()).orElseThrow());
            List<OrderResponse.Item> items = dao.findItems(orderId).stream()
                    .map(item -> {
                        Product product = jdbi.withExtension(ProductDao.class, pdao ->
                                pdao.findById(item.productId()).orElseThrow());
                        return new OrderResponse.Item(product.id(), product.name(),
                                item.quantity(), item.unitPrice());
                    })
                    .toList();
            return new OrderResponse(order.id(), customer.id(), customer.name(),
                    order.status(), order.totalAmount(), items, order.createdAt());
        });
    }
}
