package com.example.cart;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Checks the HTTP calls to product-service against a fake server. */
class ProductClientTest {

    private MockRestServiceServer server;
    private ProductClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ProductClient(builder, "http://product-service");
    }

    @Test
    void getParsesTheProduct() {
        server.expect(requestTo("http://product-service/api/products/p1"))
                .andRespond(withSuccess("""
                        {"id":"p1","name":"Laptop","price":999.99,"category":"electronics","stock":5}
                        """, MediaType.APPLICATION_JSON));

        var product = client.get("p1");

        assertThat(product.name()).isEqualTo("Laptop");
        assertThat(product.stock()).isEqualTo(5);
        server.verify();
    }

    @Test
    void notFoundIsRethrownWithProductServicesMessage() {
        server.expect(requestTo("http://product-service/api/products/missing"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Product not found: missing\"}"));

        assertThatThrownBy(() -> client.get("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found: missing");
    }
}
