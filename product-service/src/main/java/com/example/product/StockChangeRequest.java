package com.example.product;

import jakarta.validation.constraints.NotNull;

/** Positive to add stock, negative to remove it. */
public record StockChangeRequest(@NotNull Integer change) {
}
