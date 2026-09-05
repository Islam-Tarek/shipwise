package com.shipwise.auth;

import com.shipwise.common.BusinessException;
import com.shipwise.common.NotFoundException;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final Jdbi jdbi;

    AuthService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public UserAccount register(RegisterRequest request) {
        return jdbi.withExtension(UserDao.class, dao -> {
            String email = request.email().trim().toLowerCase();
            dao.findByEmail(email).ifPresent(existing -> {
                throw new BusinessException("account with email %s already exists".formatted(email));
            });
            long id = dao.insert(request.name().trim(), email, null, request.address());
            return dao.findById(id).orElseThrow();
        });
    }

    public UserAccount get(long id) {
        return jdbi.withExtension(UserDao.class, dao ->
                dao.findById(id)
                        .orElseThrow(() -> new NotFoundException("account %d not found".formatted(id)))
        );
    }

    public UserAccount getByEmail(String email) {
        return jdbi.withExtension(UserDao.class, dao ->
                dao.findByEmail(email)
                        .orElseThrow(() -> new NotFoundException("account %s not found".formatted(email)))
        );
    }
}