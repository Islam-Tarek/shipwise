package com.shipwise.order;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    Order place(@Valid @RequestBody CreateOrderRequest request) {
        return orderService.placeOrder(request);
    }

    @GetMapping("/{id}")
    Order get(@PathVariable long id) {
        return orderService.getOrder(id);
    }
}