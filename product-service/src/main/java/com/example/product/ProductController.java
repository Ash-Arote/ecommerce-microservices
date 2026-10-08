package com.example.product;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product create(@Valid @RequestBody ProductRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<Product> list(@RequestParam(required = false) String category) {
        return service.list(category);
    }

    @GetMapping("/{id}")
    public Product get(@PathVariable String id) {
        return service.get(id);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable String id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** Internal: used by order-service to reserve or release stock. */
    @PatchMapping("/{id}/stock")
    public Product changeStock(@PathVariable String id, @Valid @RequestBody StockChangeRequest request) {
        return service.changeStock(id, request.change());
    }
}
