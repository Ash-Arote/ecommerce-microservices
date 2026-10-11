package com.example.order;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Calls product-service over HTTP. */
@Component
public class ProductClient {

    private final RestClient restClient;

    @Autowired
    public ProductClient(@Value("${services.product.url}") String baseUrl) {
        this(RestClient.builder(), baseUrl);
    }

    /** Lets tests supply a builder bound to a mock server. */
    ProductClient(RestClient.Builder builder, String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    /** Positive to add stock, negative to remove it; fails with 400 if there isn't enough. */
    public void changeStock(String productId, int change) {
        DownstreamErrors.call(() -> restClient.patch()
                .uri("/api/products/{id}/stock", productId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("change", change))
                .retrieve()
                .toBodilessEntity());
    }
}
