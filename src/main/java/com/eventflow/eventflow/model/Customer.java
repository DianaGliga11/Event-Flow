package com.eventflow.eventflow.model;

import java.time.LocalDateTime;

public class Customer {
    private final long customerId;
    private final String name;
    private final String email;
    private final LocalDateTime createdAt;

    public Customer(LocalDateTime createdAt, String email, String name, long customerId) {
        this.createdAt = createdAt;
        this.email = email;
        this.name = name;
        this.customerId = customerId;
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
