package com.shipwise.inventory;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{productId}")
    Stock get(@PathVariable long productId) {
        return inventoryService.get(productId);
    }

    @PutMapping("/{productId}")
    Stock upsert(@PathVariable long productId, @Valid @RequestBody UpsertRequest request) {
        inventoryService.upsert(productId, request.quantity());
        return inventoryService.get(productId);
    }

    @PostMapping("/{productId}/reserve")
    void reserve(@PathVariable long productId, @RequestParam int quantity) {
        inventoryService.reserve(productId, quantity);
    }

    @PostMapping("/{productId}/restock")
    Stock restock(@PathVariable long productId, @RequestParam int quantity) {
        inventoryService.restock(productId, quantity);
        return inventoryService.get(productId);
    }
}