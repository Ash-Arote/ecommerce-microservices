package com.example.order;

import java.math.BigDecimal;

public record OrderItem(String productId, String name, BigDecimal price, int quantity) {

    public BigDecimal lineTotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
