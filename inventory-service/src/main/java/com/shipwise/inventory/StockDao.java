package com.shipwise.inventory;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface StockDao {

    @SqlQuery("SELECT * FROM stock WHERE product_id = :productId")
    Optional<Stock> findByProductId(@Bind("productId") long productId);

    @SqlQuery("""
            SELECT * FROM stock WHERE quantity >= :qty ORDER BY product_id LIMIT :limit
            """)
    List<Stock> findAvailable(@Bind("qty") int qty, @Bind("limit") int limit);

    @SqlQuery("SELECT * FROM stock ORDER BY product_id")
    List<Stock> findAll();

    @SqlUpdate("""
            INSERT INTO stock (product_id, quantity)
            VALUES (:productId, :quantity)
            ON CONFLICT (product_id) DO UPDATE SET quantity = :quantity, updated_at = now()
            """)
    int upsert(@Bind("productId") long productId, @Bind("quantity") int quantity);

    @SqlUpdate("UPDATE stock SET quantity = :quantity, updated_at = now() WHERE product_id = :productId")
    int setQuantity(@Bind("productId") long productId, @Bind("quantity") int quantity);

    @SqlUpdate("""
            UPDATE stock
            SET quantity = quantity - :qty, updated_at = now()
            WHERE product_id = :productId AND quantity >= :qty
            """)
    int reserve(@Bind("productId") long productId, @Bind("qty") int qty);

    @SqlUpdate("""
            UPDATE stock
            SET quantity = quantity + :qty, updated_at = now()
            WHERE product_id = :productId
            """)
    int restock(@Bind("productId") long productId, @Bind("qty") int qty);
}