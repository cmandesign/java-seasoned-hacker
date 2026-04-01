package com.owaspdemo.a05_injection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vulnerable/products")
@Tag(name = "A05 - Injection", description = "SQL injection via string concatenation + XXE via unsafe XML parsing")
public class VulnerableProductController {

    private final EntityManager entityManager;

    public VulnerableProductController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping
    @Operation(summary = "Search products (SQL injectable)", description = "Try: ' OR '1'='1  or  ' UNION SELECT id,username,password_hash,email FROM app_user --  (PostgreSQL: ' UNION SELECT id,username,password_hash,CAST(email AS VARCHAR) FROM app_user --)")
    public List<?> search(
            @Parameter(description = "Search term", example = "' OR '1'='1") @RequestParam(defaultValue = "") String search) {
        // BAD: String concatenation in SQL query — classic SQL injection
        String sql = "SELECT * FROM product WHERE name LIKE '%" + search + "%'";
        return entityManager.createNativeQuery(sql, "ProductMapping").getResultList();
    }
}
