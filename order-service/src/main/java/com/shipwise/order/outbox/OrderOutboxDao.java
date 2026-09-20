package com.shipwise.order.outbox;

import java.util.List;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface OrderOutboxDao {

    @SqlUpdate("""
            INSERT INTO order_outbox (order_id, event_type, payload)
            VALUES (:orderId, :eventType, CAST(:payload AS jsonb))
            """)
    void append(@Bind("orderId") long orderId,
                @Bind("eventType") String eventType,
                @Bind("payload") String payload);

    @SqlQuery("""
            SELECT id, order_id, event_type, payload::text AS payload,
                   created_at, sent_at, attempts
            FROM order_outbox
            WHERE sent_at IS NULL
            ORDER BY id
            FOR UPDATE SKIP LOCKED
            LIMIT :limit
            """)
    List<OrderOutbox> claimUnsent(@Bind("limit") int limit);

    @SqlUpdate("UPDATE order_outbox SET sent_at = now() WHERE id = :id")
    int markSent(@Bind("id") long id);

    @SqlUpdate("UPDATE order_outbox SET attempts = attempts + 1 WHERE id = :id")
    int markFailed(@Bind("id") long id);
}