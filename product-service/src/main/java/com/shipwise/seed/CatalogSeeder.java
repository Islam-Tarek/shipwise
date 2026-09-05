package com.shipwise.seed;

import com.shipwise.product.CategoryDao;
import com.shipwise.product.ProductDao;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

@Component
public class CatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);

    private final RestClient restClient;
    private final Jdbi jdbi;

    CatalogSeeder(@Value("${dummyjson.base-url}") String baseUrl, Jdbi jdbi) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.jdbi = jdbi;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        long productCount = jdbi.withExtension(ProductDao.class, ProductDao::countAll);
        if (productCount > 0) {
            log.info("catalog already seeded ({} products), skipping", productCount);
            return;
        }
        seed();
    }

    void seed() {
        DummyJsonPage page = restClient.get()
                .uri("/products?limit=0&select=id,title,description,category,price,stock,thumbnail")
                .retrieve()
                .body(DummyJsonPage.class);

        if (page == null || page.products() == null || page.products().isEmpty()) {
            log.warn("no products returned from dummyjson, catalog stays empty");
            return;
        }

        ensureCategories(page);

        int inserted = 0;
        int skipped = 0;
        for (DummyJsonProduct dto : page.products()) {
            Long categoryId = jdbi.withExtension(CategoryDao.class, dao ->
                    dao.findBySlug(dto.category()).map(c -> c.id()).orElse(null));
            if (categoryId == null) {
                log.warn("unknown category '{}', skipping product {}", dto.category(), dto.title());
                skipped++;
                continue;
            }
            jdbi.withExtension(ProductDao.class, dao ->
                    dao.insert(categoryId, String.valueOf(dto.id()), dto.title(), dto.description(),
                            dto.price(), dto.thumbnail(), Math.max(dto.stock(), 25)));
            inserted++;
        }
        log.info("seeded {} products from dummyjson, {} skipped", inserted, skipped);
    }

    private void ensureCategories(DummyJsonPage page) {
        long categories = page.products().stream()
                .map(DummyJsonProduct::category)
                .distinct()
                .peek(slug -> jdbi.useExtension(CategoryDao.class, dao ->
                        dao.insertIfMissing(slug, slug)))
                .count();
        log.info("ensured {} category slugs from dummyjson snapshot", categories);
    }
}