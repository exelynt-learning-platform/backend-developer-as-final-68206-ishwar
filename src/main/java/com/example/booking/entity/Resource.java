package com.example.booking.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "resources")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerHour;

    @Column(nullable = false)
    private boolean active = true;

    public Resource() {}

    public Resource(String name, String type, String description,
                     BigDecimal pricePerHour, boolean active) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.pricePerHour = pricePerHour;
        this.active = active;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getDescription() { return description; }
    public BigDecimal getPricePerHour() { return pricePerHour; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setType(String type) { this.type = type; }
    public void setDescription(String description) { this.description = description; }
    public void setPricePerHour(BigDecimal pricePerHour) { this.pricePerHour = pricePerHour; }
    public void setActive(boolean active) { this.active = active; }
}
