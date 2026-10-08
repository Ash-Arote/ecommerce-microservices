package com.example.cart;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** One cart per user; the user id doubles as the document id. */
@Document("carts")
public record Cart(@Id String userId, List<CartItem> items) {

    public BigDecimal total() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
