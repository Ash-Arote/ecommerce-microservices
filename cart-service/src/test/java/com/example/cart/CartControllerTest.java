package com.example.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.client.ResourceAccessException;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CartService service;

    @Test
    void getReturnsTheCart() {
        when(service.get("u1")).thenReturn(new Cart("u1", List.of()));

        assertThat(mvc.get().uri("/api/cart/u1"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.userId").isEqualTo("u1");
    }

    @Test
    void addItemRejectsZeroQuantity() {
        assertThat(mvc.post().uri("/api/cart/u1/items").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"p1\",\"quantity\":0}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fields.quantity").isEqualTo("must be greater than or equal to 1");
        verifyNoInteractions(service);
    }

    @Test
    void returns503WhenProductServiceIsDown() {
        when(service.get("u1")).thenThrow(new ResourceAccessException("Connection refused"));

        assertThat(mvc.get().uri("/api/cart/u1"))
                .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .bodyJson()
                .extractingPath("$.error").isEqualTo("A required service is unavailable, try again later");
    }

    @Test
    void clearReturns204() {
        assertThat(mvc.delete().uri("/api/cart/u1")).hasStatus(HttpStatus.NO_CONTENT);
        verify(service).clear("u1");
    }
}
