package com.shipwise.shipping;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipments")
public class ShippingController {

    private final ShippingService shippingService;

    ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @PostMapping
    Shipment create(@Valid @RequestBody CreateShipmentRequest request) {
        return shippingService.create(request);
    }

    @PostMapping("/{id}/ship")
    Shipment ship(@PathVariable long id) {
        return shippingService.ship(id);
    }

    @PostMapping("/{id}/cancel")
    void cancel(@PathVariable long id) {
        shippingService.cancel(id);
    }

    @GetMapping("/{id}")
    Shipment get(@PathVariable long id) {
        return shippingService.get(id);
    }
}