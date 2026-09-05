package com.shipwise.product;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.shipwise.common.NotFoundException;
import com.shipwise.common.PageResponse;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final Jdbi jdbi;

    ProductService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public PageResponse<ProductResponse> list(int page, int size, String name) {
        int offset = page * size;
        return jdbi.withExtension(ProductDao.class, dao -> {
            List<Product> products = (name == null || name.isBlank())
                    ? dao.findPage(size, offset)
                    : dao.searchByName(name.trim(), size, offset);
            long total = (name == null || name.isBlank())
                    ? dao.countAll()
                    : dao.countSearch(name.trim());
            Map<Long, Category> categories = loadCategories();
            return new PageResponse<>(
                    products.stream()
                            .map(product -> ProductResponse.from(product, slugOf(product, categories)))
                            .toList(),
                    total,
                    page,
                    size
            );
        });
    }

    public ProductResponse get(long id) {
        return jdbi.withExtension(ProductDao.class, dao ->
                dao.findById(id)
                        .map(product -> {
                            Map<Long, Category> categories = loadCategories();
                            return ProductResponse.from(product, slugOf(product, categories));
                        })
                        .orElseThrow(() -> new NotFoundException("product %d not found".formatted(id)))
        );
    }

    public void syncStock(long id, int stockQuantity) {
        int updated = jdbi.withExtension(ProductDao.class, dao -> dao.syncStock(id, stockQuantity));
        if (updated == 0) {
            throw new NotFoundException("product %d not found".formatted(id));
        }
    }

    public void syncStockByExternalId(String externalId, int stockQuantity) {
        int updated = jdbi.withExtension(ProductDao.class, dao ->
                dao.syncStockByExternalId(externalId, stockQuantity));
        if (updated == 0) {
            throw new NotFoundException("product %s not found".formatted(externalId));
        }
    }

    private Map<Long, Category> loadCategories() {
        return jdbi.withExtension(CategoryDao.class, CategoryDao::findAll).stream()
                .collect(Collectors.toMap(Category::id, Function.identity()));
    }

    private String slugOf(Product product, Map<Long, Category> categories) {
        Category category = categories.get(product.categoryId());
        return category == null ? null : category.slug();
    }
}