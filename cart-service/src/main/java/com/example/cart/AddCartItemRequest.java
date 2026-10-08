package com.example.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(@NotBlank String productId, @NotNull @Min(1) Integer quantity) {
}
