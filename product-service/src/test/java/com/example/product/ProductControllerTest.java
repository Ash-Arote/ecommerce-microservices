package com.example.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Web layer only: routing, JSON, validation and error responses. The service is mocked. */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ProductService service;

    private final Product laptop = new Product("p1", "Laptop", "14 inch", new BigDecimal("999.99"), "electronics", 5);

    @Test
    void createReturns201WithTheSavedProduct() {
        when(service.create(any())).thenReturn(laptop);

        assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Laptop","description":"14 inch","price":999.99,"category":"electronics","stock":5}
                        """))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.id").isEqualTo("p1");
    }

    @Test
    void createRejectsInvalidInputWithFieldErrors() {
        var result = assertThat(mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"","price":-1,"category":"electronics"}
                        """));

        result.hasStatus(HttpStatus.BAD_REQUEST);
        result.bodyJson().extractingPath("$.fields.name").isEqualTo("must not be blank");
        result.bodyJson().extractingPath("$.fields.price").isEqualTo("must be greater than or equal to 0.0");
        result.bodyJson().extractingPath("$.fields.stock").isEqualTo("must not be null");
        verifyNoInteractions(service);
    }

    @Test
    void getReturns404WhenProductDoesNotExist() {
        when(service.get("missing")).thenThrow(new NotFoundException("Product not found: missing"));

        assertThat(mvc.get().uri("/api/products/missing"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.error").isEqualTo("Product not found: missing");
    }

    @Test
    void listPassesTheCategoryFilter() {
        when(service.list("electronics")).thenReturn(java.util.List.of(laptop));

        assertThat(mvc.get().uri("/api/products?category=electronics"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].name").isEqualTo("Laptop");
    }

    @Test
    void deleteReturns204() {
        assertThat(mvc.delete().uri("/api/products/p1")).hasStatus(HttpStatus.NO_CONTENT);
        verify(service).delete("p1");
    }

    @Test
    void changeStockReturns400WhenNotEnoughStock() {
        when(service.changeStock("p1", -10)).thenThrow(new BadRequestException("Only 5 of Laptop in stock"));

        assertThat(mvc.patch().uri("/api/products/p1/stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"change\":-10}"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.error").isEqualTo("Only 5 of Laptop in stock");
    }
}
