package com.shipwise.inventory;

import com.shipwise.common.BusinessException;
import com.shipwise.common.NotFoundException;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final Jdbi jdbi;

    InventoryService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Stock get(long productId) {
        return jdbi.withExtension(StockDao.class, dao ->
                dao.findByProductId(productId)
                        .orElseThrow(() -> new NotFoundException("no stock for product %d".formatted(productId)))
        );
    }

    public void upsert(long productId, int quantity) {
        if (quantity < 0) {
            throw new BusinessException("quantity cannot be negative");
        }
        jdbi.useExtension(StockDao.class, dao -> dao.upsert(productId, quantity));
    }

    public void reserve(long productId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("reserve quantity must be positive");
        }
        int updated = jdbi.withExtension(StockDao.class, dao -> dao.reserve(productId, quantity));
        if (updated == 0) {
            throw new BusinessException("not enough stock for product %d".formatted(productId));
        }
        log.info("reserved {} units of product {} (one reservation wins under contention)",
                quantity, productId);
    }

    public void restock(long productId, int quantity) {
        if (quantity <= 0) {
            throw new BusinessException("restock quantity must be positive");
        }
        jdbi.useExtension(StockDao.class, dao -> dao.restock(productId, quantity));
    }
}