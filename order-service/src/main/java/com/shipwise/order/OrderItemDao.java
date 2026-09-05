package com.shipwise.order;

import java.math.BigDecimal;
import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface OrderItemDao {

    @SqlQuery("SELECT * FROM order_items WHERE order_id = :orderId ORDER BY id")
    List<OrderItem> findByOrderId(@Bind("orderId") long orderId);

    @SqlUpdate("""
            INSERT INTO order_items (order_id, product_id, product_name, unit_price, quantity)
            VALUES (:orderId, :productId, :productName, :unitPrice, :quantity)
            """)
    void insert(@Bind("orderId") long orderId,
                @Bind("productId") long productId,
                @Bind("productName") String productName,
                @Bind("unitPrice") BigDecimal unitPrice,
                @Bind("quantity") int quantity);
}