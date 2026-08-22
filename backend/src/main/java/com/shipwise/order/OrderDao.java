package com.shipwise.order;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface OrderDao {

    @SqlQuery("SELECT * FROM orders WHERE id = :id")
    Optional<Order> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM orders WHERE customer_id = :customerId ORDER BY created_at DESC")
    List<Order> findByCustomer(@Bind("customerId") long customerId);

    @SqlUpdate("""
            INSERT INTO orders (customer_id, status, total_amount)
            VALUES (:customerId, :status, :totalAmount)
            """)
    @GetGeneratedKeys
    long insert(@Bind("customerId") long customerId, @Bind("status") OrderStatus status,
                @Bind("totalAmount") BigDecimal totalAmount);

    @SqlUpdate("""
            UPDATE orders
            SET status = :status, updated_at = now()
            WHERE id = :id
            """)
    int updateStatus(@Bind("id") long id, @Bind("status") OrderStatus status);

    @SqlUpdate("""
            INSERT INTO order_items (order_id, product_id, quantity, unit_price)
            VALUES (:orderId, :productId, :quantity, :unitPrice)
            """)
    @GetGeneratedKeys
    long insertItem(@Bind("orderId") long orderId, @Bind("productId") long productId,
                    @Bind("quantity") int quantity, @Bind("unitPrice") BigDecimal unitPrice);

    @SqlQuery("""
            SELECT oi.id, oi.order_id, oi.product_id, oi.quantity, oi.unit_price
            FROM order_items oi
            WHERE oi.order_id = :orderId
            """)
    List<OrderItem> findItems(@Bind("orderId") long orderId);
}
