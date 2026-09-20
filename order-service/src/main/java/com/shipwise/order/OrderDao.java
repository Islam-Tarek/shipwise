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
    Optional<OrderRow> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM orders WHERE user_id = :userId ORDER BY id DESC")
    List<OrderRow> findByUserId(@Bind("userId") long userId);

    @SqlUpdate("""
            INSERT INTO orders (user_id, customer_name, customer_email, status)
            VALUES (:userId, :customerName, :customerEmail, 'PENDING')
            """)
    @GetGeneratedKeys
    long create(@Bind("userId") Long userId,
                @Bind("customerName") String customerName,
                @Bind("customerEmail") String customerEmail);

    @SqlUpdate("""
            UPDATE orders SET status = :status, failure_reason = :failureReason, updated_at = now()
            WHERE id = :id
            """)
    int setStatus(@Bind("id") long id, @Bind("status") String status, @Bind("failureReason") String failureReason);
}