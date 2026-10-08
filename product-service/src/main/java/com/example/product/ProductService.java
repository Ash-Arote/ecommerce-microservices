package com.example.product;

import java.util.List;

import org.springframework.stereotype.Service;


@Service
public class ProductService {

    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public Product create(ProductRequest request) {
        return repository.save(request.toProduct(null));
    }

    public List<Product> list(String category) {
        if (category == null || category.isBlank()) {
            return repository.findAll();
        }
        return repository.findByCategoryIgnoreCase(category);
    }

    public Product get(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    public Product update(String id, ProductRequest request) {
        get(id);
        return repository.save(request.toProduct(id));
    }

    public void delete(String id) {
        get(id);
        repository.deleteById(id);
    }

    public Product changeStock(String id, int change) {
        Product product = get(id);
        int newStock = product.stock() + change;
        if (newStock < 0) {
            throw new BadRequestException("Only " + product.stock() + " of " + product.name() + " in stock");
        }
        return repository.save(product.withStock(newStock));
    }
}
