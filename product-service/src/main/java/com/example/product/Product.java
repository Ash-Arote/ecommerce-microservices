package com.example.product;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("products")
public record Product(
        @Id String id,
        String name,
        String description,
        BigDecimal price,
        String category,
        int stock) {

    public Product withStock(int newStock) {
        return new Product(id, name, description, price, category, newStock);
    }
}
