package com.shipwise.shipping;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface ShipmentDao {

    @SqlQuery("SELECT * FROM shipments WHERE id = :id")
    Optional<Shipment> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM shipments WHERE order_id = :orderId")
    Optional<Shipment> findByOrderId(@Bind("orderId") long orderId);

    @SqlUpdate("""
            INSERT INTO shipments (order_id, address, carrier, tracking_number, status)
            VALUES (:orderId, :address, :carrier, :trackingNumber, 'PLANNED')
            """)
    @GetGeneratedKeys
    long create(@Bind("orderId") long orderId, @Bind("address") String address,
                @Bind("carrier") String carrier, @Bind("trackingNumber") String trackingNumber);

    @SqlUpdate("""
            UPDATE shipments SET status = :status, updated_at = now() WHERE id = :id
            """)
    int setStatus(@Bind("id") long id, @Bind("status") String status);
}