package com.shipwise.customer;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface CustomerDao {

    @SqlQuery("SELECT * FROM customers WHERE id = :id")
    Optional<Customer> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM customers WHERE email = :email")
    Optional<Customer> findByEmail(@Bind("email") String email);

    @SqlUpdate("""
            INSERT INTO customers (name, email, address)
            VALUES (:name, :email, :address)
            """)
    @GetGeneratedKeys
    long insert(@Bind("name") String name, @Bind("email") String email, @Bind("address") String address);
}
