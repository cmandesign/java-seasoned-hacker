package com.owaspdemo.a05_injection;

import jakarta.persistence.EntityManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * A05:2025 - Injection
 *
 * VULNERABLE: Builds SQL query by concatenating user input directly.
 *
 * Try: GET /api/v1/vulnerable/products?search=' OR '1'='1
 * Try: GET /api/v1/vulnerable/products?search=' UNION SELECT id, username, password_hash, email FROM app_user --
 */
@RestController
@RequestMapping("/api/v1/vulnerable/products")
public class VulnerableProductController {

    private final EntityManager entityManager;

    public VulnerableProductController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @GetMapping
    @SuppressWarnings("unchecked")
    public List<?> search(@RequestParam(defaultValue = "") String search) {
        // BAD: String concatenation in SQL query — classic SQL injection
        String sql = "SELECT * FROM product WHERE name LIKE '%" + search + "%'";
        return entityManager.createNativeQuery(sql, "ProductMapping").getResultList();
    }
}
