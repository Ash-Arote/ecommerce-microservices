package com.example.cart;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.cart.ProductClient.ProductResponse;

@Service
public class CartService {

    private final CartRepository repository;
    private final ProductClient productClient;

    public CartService(CartRepository repository, ProductClient productClient) {
        this.repository = repository;
        this.productClient = productClient;
    }

    public Cart get(String userId) {
        return repository.findById(userId).orElse(new Cart(userId, List.of()));
    }

    /** Adds the product to the cart, or increases its quantity if it is already there. */
    public Cart addItem(String userId, AddCartItemRequest request) {
        ProductResponse product = productClient.get(request.productId());
        List<CartItem> items = new ArrayList<>(get(userId).items());

        int existing = items.stream()
                .filter(i -> i.productId().equals(product.id()))
                .mapToInt(CartItem::quantity)
                .sum();
        int quantity = existing + request.quantity();
        if (quantity > product.stock()) {
            throw new BadRequestException("Only " + product.stock() + " of " + product.name() + " in stock");
        }

        items.removeIf(i -> i.productId().equals(product.id()));
        items.add(new CartItem(product.id(), product.name(), product.price(), quantity));
        return repository.save(new Cart(userId, items));
    }

    public Cart removeItem(String userId, String productId) {
        List<CartItem> items = new ArrayList<>(get(userId).items());
        if (!items.removeIf(i -> i.productId().equals(productId))) {
            throw new NotFoundException("Product " + productId + " is not in the cart");
        }
        return repository.save(new Cart(userId, items));
    }

    public void clear(String userId) {
        repository.deleteById(userId);
    }
}
