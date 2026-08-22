package com.shipwise.customer;

import com.shipwise.common.BusinessException;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Service;

@Service
public class CustomerService {

    private final Jdbi jdbi;

    CustomerService(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public Customer create(CustomerRequest request) {
        return jdbi.withExtension(CustomerDao.class, dao -> {
            String email = request.email().trim().toLowerCase();
            dao.findByEmail(email).ifPresent(existing -> {
                throw new BusinessException("customer with email %s already exists".formatted(email));
            });
            long id = dao.insert(request.name().trim(), email, request.address());
            return dao.findById(id).orElseThrow();
        });
    }

    public Customer get(long id) {
        return jdbi.withExtension(CustomerDao.class, dao ->
                dao.findById(id)
                        .orElseThrow(() -> new com.shipwise.common.NotFoundException(
                                "customer %d not found".formatted(id)))
        );
    }
}
