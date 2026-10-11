package com.example.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** Checks the HTTP calls to product-service and cart-service against a fake server. */
class ClientsTest {

    private MockRestServiceServer server;
    private ProductClient productClient;
    private CartClient cartClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        productClient = new ProductClient(builder, "http://product-service");
        cartClient = new CartClient(builder, "http://cart-service");
    }

    @Test
    void changeStockSendsAPatchWithTheChange() {
        server.expect(requestTo("http://product-service/api/products/p1/stock"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(content().json("{\"change\":-2}"))
                .andRespond(withSuccess());

        productClient.changeStock("p1", -2);

        server.verify();
    }

    @Test
    void changeStockFailureKeepsProductServicesMessage() {
        server.expect(requestTo("http://product-service/api/products/p1/stock"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":\"Only 1 of Laptop in stock\"}"));

        assertThatThrownBy(() -> productClient.changeStock("p1", -2))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only 1 of Laptop in stock");
    }

    @Test
    void getCartParsesItems() {
        server.expect(requestTo("http://cart-service/api/cart/u1"))
                .andRespond(withSuccess("""
                        {"userId":"u1","items":[{"productId":"p1","name":"Laptop","price":999.99,"quantity":2}]}
                        """, MediaType.APPLICATION_JSON));

        var cart = cartClient.get("u1");

        assertThat(cart.items()).singleElement().extracting(OrderItem::quantity).isEqualTo(2);
    }

    @Test
    void clearCartSendsADelete() {
        server.expect(requestTo("http://cart-service/api/cart/u1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        cartClient.clear("u1");

        server.verify();
    }
}
