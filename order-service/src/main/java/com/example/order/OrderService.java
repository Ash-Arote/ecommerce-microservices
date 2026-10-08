package com.example.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.order.CartClient.CartResponse;

@Service
public class OrderService {

    private final OrderRepository repository;
    private final CartClient cartClient;
    private final ProductClient productClient;

    public OrderService(OrderRepository repository, CartClient cartClient, ProductClient productClient) {
        this.repository = repository;
        this.cartClient = cartClient;
        this.productClient = productClient;
    }

    /**
     * Checks out the user's cart: reserves stock in product-service, saves the order and empties the cart.
     * There is no transaction across services, so if any reservation fails the ones already made are
     * released again (a compensating action).
     */
    public Order place(String userId) {
        CartResponse cart = cartClient.get(userId);
        if (cart.items().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        List<OrderItem> reserved = new ArrayList<>();
        try {
            for (OrderItem item : cart.items()) {
                productClient.changeStock(item.productId(), -item.quantity());
                reserved.add(item);
            }
        } catch (RuntimeException e) {
            for (OrderItem item : reserved) {
                productClient.changeStock(item.productId(), item.quantity());
            }
            throw e;
        }

        BigDecimal total = cart.items().stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        Order order = repository.save(new Order(null, userId, cart.items(), total, Instant.now()));
        cartClient.clear(userId);
        return order;
    }

    public Order get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }
}
