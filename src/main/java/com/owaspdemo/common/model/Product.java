package com.owaspdemo.common.model;

import jakarta.persistence.*;

@Entity
@Table(name = "product")
@SqlResultSetMapping(name = "ProductMapping", entities = @EntityResult(entityClass = Product.class))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private String price;

    public Product() {}

    public Product(String name, String description, String price) {
        this.name = name;
        this.description = description;
        this.price = price;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }
}
