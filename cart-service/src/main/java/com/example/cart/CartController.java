package com.example.cart;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart/{userId}")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public Cart get(@PathVariable String userId) {
        return service.get(userId);
    }

    @PostMapping("/items")
    public Cart addItem(@PathVariable String userId, @Valid @RequestBody AddCartItemRequest request) {
        return service.addItem(userId, request);
    }

    @DeleteMapping("/items/{productId}")
    public Cart removeItem(@PathVariable String userId, @PathVariable String productId) {
        return service.removeItem(userId, productId);
    }

    /** Internal: used by order-service to empty the cart after checkout. */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@PathVariable String userId) {
        service.clear(userId);
    }
}
