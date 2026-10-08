package com.example.cart;

import java.math.BigDecimal;

public record CartItem(String productId, String name, BigDecimal price, int quantity) {

    public BigDecimal lineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
