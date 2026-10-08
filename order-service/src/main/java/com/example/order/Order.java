package com.example.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("orders")
public record Order(
        @Id String id,
        String userId,
        List<OrderItem> items,
        BigDecimal total,
        Instant createdAt) {
}
