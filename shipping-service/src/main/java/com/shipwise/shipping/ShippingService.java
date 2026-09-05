package com.shipwise.shipping;

import com.shipwise.common.BusinessException;
import com.shipwise.common.NotFoundException;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ShippingService {

    private static final Logger log = LoggerFactory.getLogger(ShippingService.class);

    private final Jdbi jdbi;

    ShippingService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Shipment create(CreateShipmentRequest request) {
        return jdbi.withExtension(ShipmentDao.class, dao -> {
            dao.findByOrderId(request.orderId()).ifPresent(existing -> {
                throw new BusinessException("order %d already has a shipment".formatted(request.orderId()));
            });

            String tracking = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            long id = dao.create(request.orderId(), request.address(), request.carrier(), tracking);
            Shipment shipment = dao.findById(id).orElseThrow();
            log.info("created shipment {} for order {} via {}", id, request.orderId(), shipment.carrier());
            return shipment;
        });
    }

    public Shipment ship(long id) {
        Shipment shipment = get(id);
        int updated = jdbi.withExtension(ShipmentDao.class, dao -> dao.setStatus(shipment.id(), "SHIPPED"));
        if (updated == 0) {
            throw new BusinessException("shipment %d not found".formatted(id));
        }
        log.info("shipment {} marked SHIPPED", id);
        return get(id);
    }

    public void cancel(long id) {
        Shipment shipment = get(id);
        if ("SHIPPED".equals(shipment.status())) {
            throw new BusinessException("shipment %d is already SHIPPED and cannot be cancelled".formatted(id));
        }
        jdbi.useExtension(ShipmentDao.class, dao -> dao.setStatus(shipment.id(), "CANCELLED"));
        log.info("shipment {} cancelled", id);
    }

    public Shipment get(long id) {
        return jdbi.withExtension(ShipmentDao.class, dao ->
                dao.findById(id).orElseThrow(() -> new NotFoundException("shipment %d not found".formatted(id)))
        );
    }
}