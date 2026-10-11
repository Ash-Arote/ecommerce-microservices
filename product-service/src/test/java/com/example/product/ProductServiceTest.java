package com.example.product;

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

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    private final Product laptop = new Product("p1", "Laptop", "14 inch", new BigDecimal("999.99"), "electronics", 5);

    @Test
    void listWithoutCategoryReturnsAllProducts() {
        when(repository.findAll()).thenReturn(List.of(laptop));

        assertThat(service.list(null)).containsExactly(laptop);
    }

    @Test
    void listWithCategoryFiltersByCategory() {
        when(repository.findByCategoryIgnoreCase("Electronics")).thenReturn(List.of(laptop));

        assertThat(service.list("Electronics")).containsExactly(laptop);
        verify(repository, never()).findAll();
    }

    @Test
    void getThrowsWhenProductDoesNotExist() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found: missing");
    }

    @Test
    void updateKeepsTheExistingId() {
        when(repository.findById("p1")).thenReturn(Optional.of(laptop));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product updated = service.update("p1",
                new ProductRequest("Laptop Pro", "14 inch", new BigDecimal("1199.00"), "electronics", 3));

        assertThat(updated.id()).isEqualTo("p1");
        assertThat(updated.name()).isEqualTo("Laptop Pro");
    }

    @Test
    void updateDoesNotSaveWhenProductDoesNotExist() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update("missing",
                new ProductRequest("X", null, BigDecimal.ONE, "misc", 1)))
                .isInstanceOf(NotFoundException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteThrowsWhenProductDoesNotExist() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("missing")).isInstanceOf(NotFoundException.class);
        verify(repository, never()).deleteById(any());
    }

    @Test
    void changeStockAppliesTheChange() {
        when(repository.findById("p1")).thenReturn(Optional.of(laptop));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.changeStock("p1", -2).stock()).isEqualTo(3);
        assertThat(service.changeStock("p1", 4).stock()).isEqualTo(9);
    }

    @Test
    void changeStockRejectsGoingBelowZero() {
        when(repository.findById("p1")).thenReturn(Optional.of(laptop));

        assertThatThrownBy(() -> service.changeStock("p1", -6))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 5 of Laptop in stock");
        verify(repository, never()).save(any());
    }
}
