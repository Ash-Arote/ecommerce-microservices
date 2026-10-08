package com.example.order;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls cart-service over HTTP. */
@Component
public class CartClient {

    public record CartResponse(String userId, List<OrderItem> items) {
    }

    private final RestClient restClient;

    public CartClient(@Value("${services.cart.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public CartResponse get(String userId) {
        return DownstreamErrors.call(() -> restClient.get()
                .uri("/api/cart/{userId}", userId)
                .retrieve()
                .body(CartResponse.class));
    }

    public void clear(String userId) {
        DownstreamErrors.call(() -> restClient.delete()
                .uri("/api/cart/{userId}", userId)
                .retrieve()
                .toBodilessEntity());
    }
}
