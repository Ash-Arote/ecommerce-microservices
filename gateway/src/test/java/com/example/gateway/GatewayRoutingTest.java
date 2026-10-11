package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.sun.net.httpserver.HttpServer;

/** Starts three fake services and checks that the gateway sends each path to the right one. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    private static final HttpServer PRODUCT = fakeService("product-service");
    private static final HttpServer CART = fakeService("cart-service");
    private static final HttpServer ORDER = fakeService("order-service");

    @LocalServerPort
    private int port;

    private final HttpClient http = HttpClient.newHttpClient();

    /** A tiny HTTP server that answers every request with "<name> <path>". */
    private static HttpServer fakeService(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = (name + " " + exchange.getRequestURI().getPath()).getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @DynamicPropertySource
    static void serviceUrls(DynamicPropertyRegistry registry) {
        registry.add("PRODUCT_SERVICE_URL", () -> "http://localhost:" + PRODUCT.getAddress().getPort());
        registry.add("CART_SERVICE_URL", () -> "http://localhost:" + CART.getAddress().getPort());
        registry.add("ORDER_SERVICE_URL", () -> "http://localhost:" + ORDER.getAddress().getPort());
    }

    @AfterAll
    static void stopFakeServices() {
        PRODUCT.stop(0);
        CART.stop(0);
        ORDER.stop(0);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void productPathsGoToProductService() throws Exception {
        assertThat(get("/api/products").body()).isEqualTo("product-service /api/products");
        assertThat(get("/api/products/p1").body()).isEqualTo("product-service /api/products/p1");
    }

    @Test
    void cartPathsGoToCartService() throws Exception {
        assertThat(get("/api/cart/u1").body()).isEqualTo("cart-service /api/cart/u1");
    }

    @Test
    void orderPathsGoToOrderService() throws Exception {
        assertThat(get("/api/orders/o1").body()).isEqualTo("order-service /api/orders/o1");
    }

    @Test
    void unknownPathsAreNotRouted() throws Exception {
        assertThat(get("/api/unknown").statusCode()).isEqualTo(404);
    }
}
