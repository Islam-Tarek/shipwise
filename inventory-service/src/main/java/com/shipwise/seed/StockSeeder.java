package com.shipwise.seed;

import com.shipwise.inventory.StockDao;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class StockSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StockSeeder.class);

    private final RestClient restClient;
    private final Jdbi jdbi;

    StockSeeder(RestClient.Builder builder, @Value("${dummyjson.base-url}") String baseUrl, Jdbi jdbi) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.jdbi = jdbi;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasStock = jdbi.withExtension(StockDao.class, dao ->
                dao.findAvailable(1, 1).size() > 0);
        if (hasStock) {
            log.info("stock already seeded, skipping");
            return;
        }
        seed();
    }

    void seed() {
        DummyJsonPage page = restClient.get()
                .uri("/products?limit=0&select=id,stock")
                .retrieve()
                .body(DummyJsonPage.class);

        if (page == null || page.products() == null || page.products().isEmpty()) {
            log.warn("no products returned from dummyjson, stock stays empty");
            return;
        }

        int upserted = 0;
        for (DummyJsonStock dto : page.products()) {
            int quantity = Math.max(dto.stock(), 25);
            jdbi.withExtension(StockDao.class, dao -> dao.upsert(dto.id(), quantity));
            upserted++;
        }
        log.info("seeded {} stock rows from dummyjson", upserted);
    }
}