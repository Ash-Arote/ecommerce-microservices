package com.example.product;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import tools.jackson.databind.json.JsonMapper;

/** Full application against a real MongoDB (Testcontainers). Requires Docker. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ProductApiIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ProductRepository repository;

    private final JsonMapper json = JsonMapper.builder().build();

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private String createProduct(String name, String category, int stock) throws Exception {
        var result = mvc.post().uri("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","price":10.00,"category":"%s","stock":%d}
                        """.formatted(name, category, stock))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return json.readTree(result.getResponse().getContentAsString()).get("id").asString();
    }

    @Test
    void createdProductCanBeReadBack() throws Exception {
        String id = createProduct("Laptop", "electronics", 5);

        assertThat(mvc.get().uri("/api/products/" + id))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name").isEqualTo("Laptop");
    }

    @Test
    void categoryFilterIgnoresCase() throws Exception {
        createProduct("Laptop", "electronics", 5);
        createProduct("Mug", "kitchen", 20);

        var result = assertThat(mvc.get().uri("/api/products?category=ELECTRONICS")).hasStatusOk();
        result.bodyJson().extractingPath("$.length()").isEqualTo(1);
        result.bodyJson().extractingPath("$[0].name").isEqualTo("Laptop");
    }

    @Test
    void stockChangeIsSaved() throws Exception {
        String id = createProduct("Laptop", "electronics", 5);

        assertThat(mvc.patch().uri("/api/products/" + id + "/stock").contentType(MediaType.APPLICATION_JSON)
                .content("{\"change\":-2}")).hasStatusOk();

        assertThat(repository.findById(id)).get().extracting(Product::stock).isEqualTo(3);
    }

    @Test
    void deletedProductIsGone() throws Exception {
        String id = createProduct("Laptop", "electronics", 5);

        assertThat(mvc.delete().uri("/api/products/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/products/" + id)).hasStatus(HttpStatus.NOT_FOUND);
    }
}
