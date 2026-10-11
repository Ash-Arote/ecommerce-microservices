package com.example.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.order.CartClient.CartResponse;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository repository;

    @Mock
    private CartClient cartClient;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderService service;

    private final OrderItem laptop = new OrderItem("p1", "Laptop", new BigDecimal("1000.00"), 2);
    private final OrderItem mug = new OrderItem("p2", "Mug", new BigDecimal("8.50"), 1);

    @Test
    void placeRejectsAnEmptyCart() {
        when(cartClient.get("u1")).thenReturn(new CartResponse("u1", List.of()));

        assertThatThrownBy(() -> service.place("u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cart is empty");
        verify(productClient, never()).changeStock(anyString(), anyInt());
        verify(repository, never()).save(any());
    }

    @Test
    void placeReservesStockSavesTheOrderAndEmptiesTheCart() {
        when(cartClient.get("u1")).thenReturn(new CartResponse("u1", List.of(laptop, mug)));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order order = service.place("u1");

        assertThat(order.userId()).isEqualTo("u1");
        assertThat(order.items()).containsExactly(laptop, mug);
        assertThat(order.total()).isEqualByComparingTo("2008.50");
        assertThat(order.createdAt()).isNotNull();

        InOrder steps = inOrder(productClient, repository, cartClient);
        steps.verify(productClient).changeStock("p1", -2);
        steps.verify(productClient).changeStock("p2", -1);
        steps.verify(repository).save(any());
        steps.verify(cartClient).clear("u1");
    }

    @Test
    void placeReleasesReservedStockWhenALaterItemIsOutOfStock() {
        when(cartClient.get("u1")).thenReturn(new CartResponse("u1", List.of(laptop, mug)));
        // lenient: changeStock is also (deliberately) called with other arguments in this test.
        lenient().doThrow(new BadRequestException("Only 0 of Mug in stock")).when(productClient).changeStock("p2", -1);

        assertThatThrownBy(() -> service.place("u1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 0 of Mug in stock");

        // The laptops reserved before the failure are put back.
        verify(productClient).changeStock("p1", -2);
        verify(productClient).changeStock("p1", 2);
        verify(repository, never()).save(any());
        verify(cartClient, never()).clear(anyString());
    }

    @Test
    void getThrowsWhenOrderDoesNotExist() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order not found: missing");
    }
}
