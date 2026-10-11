package com.example.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private OrderService service;

    @Test
    void placeReturns201WithTheOrder() {
        when(service.place("u1")).thenReturn(new Order("o1", "u1",
                List.of(new OrderItem("p1", "Laptop", new BigDecimal("1000.00"), 1)),
                new BigDecimal("1000.00"), Instant.now()));

        assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("{\"userId\":\"u1\"}"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id").isEqualTo("o1");
    }

    @Test
    void placeRequiresAUserId() {
        assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.fields.userId").isEqualTo("must not be blank");
        verifyNoInteractions(service);
    }

    @Test
    void getReturns404WhenOrderDoesNotExist() {
        when(service.get("missing")).thenThrow(new NotFoundException("Order not found: missing"));

        assertThat(mvc.get().uri("/api/orders/missing")).hasStatus(HttpStatus.NOT_FOUND);
    }
}
