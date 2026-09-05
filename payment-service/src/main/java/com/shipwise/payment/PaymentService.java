package com.shipwise.payment;

import com.shipwise.common.BusinessException;
import com.shipwise.common.NotFoundException;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final Jdbi jdbi;
    private final double failureRate;
    private final SecureRandom random = new SecureRandom();

    PaymentService(Jdbi jdbi, @Value("${payment.failure-rate:0.2}") double failureRate) {
        this.jdbi = jdbi;
        this.failureRate = failureRate;
    }

    public Payment charge(CreatePaymentRequest request) {
        return jdbi.withExtension(PaymentDao.class, dao -> {
            dao.latestForOrder(request.orderId()).ifPresent(existing -> {
                if (existing.succeeded()) {
                    throw new BusinessException("order %d already paid".formatted(request.orderId()));
                }
            });

            long id = dao.create(request.orderId(), request.amount(), request.currency().toUpperCase());
            boolean fail = random.nextDouble() < failureRate;
            dao.resolve(id, fail ? "FAILED" : "SUCCEEDED",
                    fail ? "card declined by mock processor" : null);
            Payment payment = dao.findById(id).orElseThrow();
            log.info("payment {} for order {} {} (rate {})",
                    id, request.orderId(), payment.succeeded() ? "succeeded" : "failed", failureRate);
            if (!payment.succeeded()) {
                throw new PaymentFailedException(id, "payment failed: " + payment.failureReason());
            }
            return payment;
        });
    }

    public void refund(long paymentId) {
        int updated = jdbi.withExtension(PaymentDao.class, dao -> {
            Payment payment = dao.findById(paymentId)
                    .orElseThrow(() -> new NotFoundException("payment %d not found".formatted(paymentId)));
            if (!payment.succeeded()) {
                return -1;
            }
            return dao.resolve(paymentId, "REFUNDED", null);
        });
        if (updated < 0) {
            throw new BusinessException("payment %d is not refundable".formatted(paymentId));
        }
    }

    public Payment get(long id) {
        return jdbi.withExtension(PaymentDao.class, dao ->
                dao.findById(id).orElseThrow(() -> new NotFoundException("payment %d not found".formatted(id)))
        );
    }
}