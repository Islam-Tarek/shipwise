package com.shipwise.notification;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface NotificationDao {

    @SqlUpdate("""
            INSERT INTO notifications (type, recipient, message, channel)
            VALUES (:type, :recipient, :message, :channel)
            """)
    @GetGeneratedKeys
    long insert(@Bind("type") String type, @Bind("recipient") String recipient,
                @Bind("message") String message, @Bind("channel") String channel);

    @SqlQuery("SELECT count(*) FROM notifications WHERE order_id = :orderId")
    int countByOrderId(@Bind("orderId") long orderId);

    @SqlUpdate("""
            INSERT INTO notifications (order_id, type, recipient, message, channel)
            VALUES (:orderId, :type, :recipient, :message, :channel)
            ON CONFLICT (order_id) WHERE order_id IS NOT NULL DO NOTHING
            """)
    int insertForOrder(@Bind("orderId") long orderId, @Bind("type") String type,
                       @Bind("recipient") String recipient, @Bind("message") String message,
                       @Bind("channel") String channel);
}