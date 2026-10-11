package com.example.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.cart.ProductClient.ProductResponse;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository repository;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private CartService service;

    private final ProductResponse laptop = new ProductResponse("p1", "Laptop", new BigDecimal("999.99"), 5);

    @Test
    void getReturnsAnEmptyCartForNewUsers() {
        when(repository.findById("u1")).thenReturn(Optional.empty());

        Cart cart = service.get("u1");

        assertThat(cart.items()).isEmpty();
        assertThat(cart.total()).isEqualByComparingTo("0");
    }

    @Test
    void addItemCopiesNameAndPriceFromProductService() {
        when(productClient.get("p1")).thenReturn(laptop);
        when(repository.findById("u1")).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = service.addItem("u1", new AddCartItemRequest("p1", 2));

        assertThat(cart.items()).containsExactly(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 2));
        assertThat(cart.total()).isEqualByComparingTo("1999.98");
    }

    @Test
    void addingTheSameProductAgainIncreasesTheQuantity() {
        when(productClient.get("p1")).thenReturn(laptop);
        when(repository.findById("u1")).thenReturn(Optional.of(
                new Cart("u1", List.of(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 2)))));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Cart cart = service.addItem("u1", new AddCartItemRequest("p1", 1));

        assertThat(cart.items()).singleElement().extracting(CartItem::quantity).isEqualTo(3);
    }

    @Test
    void addItemRejectsMoreThanTheAvailableStock() {
        when(productClient.get("p1")).thenReturn(laptop);
        when(repository.findById("u1")).thenReturn(Optional.of(
                new Cart("u1", List.of(new CartItem("p1", "Laptop", new BigDecimal("999.99"), 4)))));

        assertThatThrownBy(() -> service.addItem("u1", new AddCartItemRequest("p1", 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 5 of Laptop in stock");
        verify(repository, never()).save(any());
    }

    @Test
    void addItemFailsWhenProductDoesNotExist() {
        when(productClient.get("missing")).thenThrow(new NotFoundException("Product not found: missing"));

        assertThatThrownBy(() -> service.addItem("u1", new AddCartItemRequest("missing", 1)))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void removeItemThrowsWhenProductIsNotInTheCart() {
        when(repository.findById("u1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.removeItem("u1", "p1"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product p1 is not in the cart");
    }
}
