package com.shipwise.product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface ProductDao {

    @SqlQuery("SELECT * FROM products WHERE id = :id")
    Optional<Product> findById(@Bind("id") long id);

    @SqlQuery("""
            SELECT * FROM products
            WHERE is_active = true
            ORDER BY id
            LIMIT :limit OFFSET :offset
            """)
    List<Product> findPage(@Bind("limit") int limit, @Bind("offset") int offset);

    @SqlQuery("SELECT count(*) FROM products WHERE is_active = true")
    long countAll();

    @SqlQuery("""
            SELECT * FROM products
            WHERE is_active = true AND name ILIKE '%' || :term || '%'
            ORDER BY name
            LIMIT :limit OFFSET :offset
            """)
    List<Product> searchByName(@Bind("term") String term, @Bind("limit") int limit, @Bind("offset") int offset);

    @SqlQuery("SELECT count(*) FROM products WHERE is_active = true AND name ILIKE '%' || :term || '%'")
    long countSearch(@Bind("term") String term);

    @SqlQuery("SELECT * FROM products WHERE external_id = :externalId")
    Optional<Product> findByExternalId(@Bind("externalId") String externalId);

    @SqlUpdate("""
            INSERT INTO products (category_id, external_id, name, description, price, image_url, stock_quantity)
            VALUES (:categoryId, :externalId, :name, :description, :price, :imageUrl, :stockQuantity)
            """)
    @GetGeneratedKeys
    long insert(@Bind("categoryId") long categoryId,
                @Bind("externalId") String externalId,
                @Bind("name") String name,
                @Bind("description") String description,
                @Bind("price") BigDecimal price,
                @Bind("imageUrl") String imageUrl,
                @Bind("stockQuantity") int stockQuantity);

    @SqlUpdate("""
            UPDATE products
            SET category_id = :categoryId,
                name = :name,
                description = :description,
                price = :price,
                image_url = :imageUrl,
                stock_quantity = :stockQuantity,
                updated_at = now()
            WHERE external_id = :externalId
            """)
    int updateByExternalId(@Bind("categoryId") long categoryId,
                           @Bind("externalId") String externalId,
                           @Bind("name") String name,
                           @Bind("description") String description,
                           @Bind("price") BigDecimal price,
                           @Bind("imageUrl") String imageUrl,
                           @Bind("stockQuantity") int stockQuantity);

    @SqlUpdate("""
            UPDATE products
            SET stock_quantity = :stockQuantity, updated_at = now()
            WHERE id = :id
            """)
    int syncStock(@Bind("id") long id, @Bind("stockQuantity") int stockQuantity);
}