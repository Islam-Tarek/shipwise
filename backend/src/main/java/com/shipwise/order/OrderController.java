package com.shipwise.order;

import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OrderResponse checkout(@Valid @RequestBody CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @GetMapping("/{id}")
    OrderResponse get(@PathVariable long id) {
        return orderService.get(id);
    }

    @GetMapping
    List<OrderResponse> listByCustomer(@RequestParam long customerId) {
        return orderService.listByCustomer(customerId);
    }

    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable long id) {
        return orderService.cancel(id);
    }
}
