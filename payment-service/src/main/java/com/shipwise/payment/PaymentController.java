package com.shipwise.payment;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    Payment charge(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.charge(request);
    }

    @PostMapping("/{id}/refund")
    void refund(@PathVariable long id) {
        paymentService.refund(id);
    }

    @GetMapping("/{id}")
    Payment get(@PathVariable long id) {
        return paymentService.get(id);
    }
}