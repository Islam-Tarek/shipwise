package com.shipwise.payment;

import java.math.BigDecimal;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface PaymentDao {

    @SqlQuery("SELECT * FROM payments WHERE id = :id")
    Optional<Payment> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM payments WHERE order_id = :orderId ORDER BY id DESC")
    Optional<Payment> latestForOrder(@Bind("orderId") long orderId);

    @SqlUpdate("""
            INSERT INTO payments (order_id, amount, currency, status)
            VALUES (:orderId, :amount, :currency, 'PENDING')
            """)
    @GetGeneratedKeys
    long create(@Bind("orderId") long orderId,
                @Bind("amount") BigDecimal amount,
                @Bind("currency") String currency);

    @SqlUpdate("""
            UPDATE payments
            SET status = :status, failure_reason = :failureReason, updated_at = now()
            WHERE id = :id
            """)
    int resolve(@Bind("id") long id, @Bind("status") String status, @Bind("failureReason") String failureReason);
}