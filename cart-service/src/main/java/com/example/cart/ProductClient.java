package com.example.cart;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls product-service over HTTP. */
@Component
public class ProductClient {

    public record ProductResponse(String id, String name, BigDecimal price, int stock) {
    }

    private final RestClient restClient;

    public ProductClient(@Value("${services.product.url}") String baseUrl) {
        this.restClient = RestClient.create(baseUrl);
    }

    public ProductResponse get(String id) {
        return DownstreamErrors.call(() -> restClient.get()
                .uri("/api/products/{id}", id)
                .retrieve()
                .body(ProductResponse.class));
    }
}
