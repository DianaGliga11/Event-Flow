package com.eventflow.eventflow.model;

import java.time.LocalDateTime;

public class Customer {
    private final long customerId;
    private final String name;
    private final String email;
    private final LocalDateTime createdAt;

    public Customer(long customerId, String email, String name, LocalDateTime createdAt) {
        this.customerId = customerId;
        this.email = email;
        this.name = name;
        this.createdAt = createdAt;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
