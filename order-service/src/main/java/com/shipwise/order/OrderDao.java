package com.shipwise.order;

import java.math.BigDecimal;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface OrderDao {

    @SqlQuery("SELECT * FROM orders WHERE id = :id")
    Optional<OrderRow> findById(@Bind("id") long id);

    @SqlUpdate("""
            INSERT INTO orders (customer_name, customer_email, status)
            VALUES (:customerName, :customerEmail, 'PENDING')
            """)
    @GetGeneratedKeys
    long create(@Bind("customerName") String customerName, @Bind("customerEmail") String customerEmail);

    @SqlUpdate("""
            UPDATE orders SET status = :status, failure_reason = :failureReason, updated_at = now()
            WHERE id = :id
            """)
    int setStatus(@Bind("id") long id, @Bind("status") String status, @Bind("failureReason") String failureReason);
}