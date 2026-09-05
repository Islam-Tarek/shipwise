package com.shipwise.notification;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface NotificationDao {

    @SqlUpdate("""
            INSERT INTO notifications (type, recipient, message, channel)
            VALUES (:type, :recipient, :message, :channel)
            """)
    @GetGeneratedKeys
    long insert(@Bind("type") String type, @Bind("recipient") String recipient,
                @Bind("message") String message, @Bind("channel") String channel);
}