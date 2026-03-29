package com.owaspdemo.a05_injection;

import com.owaspdemo.common.model.Product;
import com.owaspdemo.common.repository.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/secure/products")
@Tag(name = "A05 - Injection", description = "Parameterized queries + DTD-disabled XML parsing")
public class SecureProductController {

    private final ProductRepository productRepository;

    public SecureProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    @Operation(summary = "Search products (parameterized)", description = "Same payload treated as literal string — 0 results")
    public List<Product> search(
            @Parameter(description = "Search term", example = "MacBook") @RequestParam(defaultValue = "") String search) {
        // GOOD: Parameterized query — input is never part of SQL syntax
        return productRepository.searchByName(search);
    }
}
