package com.shipwise.product;

import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {

    private final Jdbi jdbi;

    CategoryService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public List<Category> findAll() {
        return jdbi.withExtension(CategoryDao.class, CategoryDao::findAll);
    }
}