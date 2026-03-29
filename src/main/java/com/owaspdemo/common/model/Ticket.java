package com.owaspdemo.common.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ticket")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String eventName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** JSON array of ticket holder names, e.g. ["Alice Smith","Bob Jones"] */
    @Column(columnDefinition = "CLOB")
    private String ticketHolders;

    @Column(nullable = false)
    private Instant purchasedAt = Instant.now();

    public Ticket() {}

    public Ticket(Long userId, String eventName, int quantity, BigDecimal price, String ticketHolders) {
        this.userId = userId;
        this.eventName = eventName;
        this.quantity = quantity;
        this.price = price;
        this.ticketHolders = ticketHolders;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getTicketHolders() { return ticketHolders; }
    public void setTicketHolders(String ticketHolders) { this.ticketHolders = ticketHolders; }
    public Instant getPurchasedAt() { return purchasedAt; }
    public void setPurchasedAt(Instant purchasedAt) { this.purchasedAt = purchasedAt; }
}
