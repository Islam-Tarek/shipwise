package com.shipwise.product;

import java.util.List;
import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

public interface CategoryDao {

    @SqlQuery("SELECT * FROM categories ORDER BY name")
    List<Category> findAll();

    @SqlQuery("SELECT * FROM categories WHERE slug = :slug")
    Optional<Category> findBySlug(@Bind("slug") String slug);
}