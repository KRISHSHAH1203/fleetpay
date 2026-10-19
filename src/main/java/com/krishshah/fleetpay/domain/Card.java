package com.krishshah.fleetpay.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

/** A fleet card issued to a driver, with a daily spend limit. */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String driverName;

    @Column(nullable = false, unique = true, length = 4)
    private String last4;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyLimit;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Card() { }

    public Card(String driverName, String last4, BigDecimal dailyLimit) {
        this.driverName = driverName;
        this.last4 = last4;
        this.dailyLimit = dailyLimit;
    }

    public Long getId() { return id; }
    public String getDriverName() { return driverName; }
    public String getLast4() { return last4; }
    public BigDecimal getDailyLimit() { return dailyLimit; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }

    public void deactivate() { this.active = false; }
    public void updateDailyLimit(BigDecimal newLimit) { this.dailyLimit = newLimit; }
}
