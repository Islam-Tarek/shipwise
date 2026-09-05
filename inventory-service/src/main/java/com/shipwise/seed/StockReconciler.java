package com.shipwise.seed;

import com.shipwise.inventory.Stock;
import com.shipwise.inventory.StockDao;
import com.shipwise.inventory.StockSyncService;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "seed-sync.enabled", havingValue = "true", matchIfMissing = true)
public class StockReconciler implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockReconciler.class);

    private final Jdbi jdbi;
    private final StockSyncService stockSync;

    StockReconciler(Jdbi jdbi, StockSyncService stockSync) {
        this.jdbi = jdbi;
        this.stockSync = stockSync;
    }

    @Override
    public void run(ApplicationArguments args) {
        java.util.List<Stock> all = jdbi.withExtension(StockDao.class, StockDao::findAll);
        log.info("reconciling {} stock rows with product service", all.size());
        for (Stock stock : all) {
            stockSync.push(stock.productId(), stock.quantity());
        }
    }
}