package com.owaspdemo.config;

import com.owaspdemo.common.model.AppUser;
import com.owaspdemo.common.model.Product;
import com.owaspdemo.common.model.Role;
import com.owaspdemo.common.model.Ticket;
import com.owaspdemo.common.repository.ProductRepository;
import com.owaspdemo.common.repository.TicketRepository;
import com.owaspdemo.common.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, ProductRepository productRepository,
                           TicketRepository ticketRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        userRepository.saveAll(List.of(
            new AppUser("admin", passwordEncoder.encode("Admin123!"), "admin@company.com", "123-45-6789", Role.ADMIN, "System", "Admin", "+1-555-100-0001"),
            new AppUser("alice", passwordEncoder.encode("Alice123!"), "alice@company.com", "987-65-4321", Role.USER, "Alice", "Johnson", "+1-555-100-0002"),
            new AppUser("bob",   passwordEncoder.encode("Bob12345!"), "bob@company.com",   "555-12-3456", Role.USER, "Bob", "Smith", "+1-555-100-0003"),
            new AppUser("manager", passwordEncoder.encode("Mgr12345!"), "manager@company.com", "111-22-3333", Role.MANAGER, "Mark", "Thompson", "+1-555-100-0004")
        ));

        productRepository.saveAll(List.of(
            new Product("MacBook Pro 16\"",   "Apple M3 Max, 36GB RAM",       new BigDecimal("3499.00")),
            new Product("ThinkPad X1 Carbon", "Intel i7, 32GB RAM, 1TB SSD",  new BigDecimal("1899.00")),
            new Product("Dell XPS 15",        "Intel i9, 64GB RAM",           new BigDecimal("2599.00")),
            new Product("Magic Keyboard",     "Apple wireless keyboard",       new BigDecimal("299.00")),
            new Product("Samsung Monitor 32\"","4K UHD, USB-C",               new BigDecimal("549.00")),
            new Product("Logitech MX Master", "Wireless ergonomic mouse",     new BigDecimal("99.00")),
            new Product("AirPods Pro",        "Active noise cancellation",    new BigDecimal("249.00")),
            new Product("Standing Desk",      "Electric adjustable height",   new BigDecimal("699.00"))
        ));

        // Seed sample tickets — userId 2 = alice, userId 3 = bob
        ticketRepository.saveAll(List.of(
            new Ticket(2L, "Spring Conference 2026", 3, new BigDecimal("150.00"),
                    "[\"Alice Johnson\",\"Charlie Brown\",\"Dana White\"]"),
            new Ticket(2L, "Java Summit", 1, new BigDecimal("299.00"),
                    "[\"Alice Johnson\"]"),
            new Ticket(3L, "DevOps Days", 2, new BigDecimal("99.00"),
                    "[\"Bob Smith\",\"Eve Martinez\"]")
        ));
    }
}
