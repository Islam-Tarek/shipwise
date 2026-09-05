package com.shipwise.auth;

import java.util.Optional;

import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

public interface UserDao {

    @SqlQuery("SELECT * FROM users WHERE id = :id")
    Optional<UserAccount> findById(@Bind("id") long id);

    @SqlQuery("SELECT * FROM users WHERE email = :email")
    Optional<UserAccount> findByEmail(@Bind("email") String email);

    @SqlUpdate("""
            INSERT INTO users (name, email, password_hash, address)
            VALUES (:name, :email, :passwordHash, :address)
            """)
    @GetGeneratedKeys
    long insert(@Bind("name") String name, @Bind("email") String email,
                @Bind("passwordHash") String passwordHash, @Bind("address") String address);

    @SqlUpdate("UPDATE users SET password_hash = :hash, updated_at = now() WHERE id = :id")
    int setPassword(@Bind("id") long id, @Bind("hash") String hash);
}