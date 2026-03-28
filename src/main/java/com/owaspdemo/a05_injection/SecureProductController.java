package com.owaspdemo.a05_injection;

import com.owaspdemo.common.model.Product;
import com.owaspdemo.common.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A05:2025 - Injection
 *
 * SECURE: Uses Spring Data JPA @Query with named parameters.
 * The same malicious input is treated as a literal string, not SQL.
 *
 * Try: GET /api/v1/secure/products?search=' OR '1'='1
 * Result: 0 products (the literal string doesn't match any name)
 */
@RestController
@RequestMapping("/api/v1/secure/products")
public class SecureProductController {

    private final ProductRepository productRepository;

    public SecureProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<Product> search(@RequestParam(defaultValue = "") String search) {
        // GOOD: Parameterized query — input is never part of SQL syntax
        return productRepository.searchByName(search);
    }
}
