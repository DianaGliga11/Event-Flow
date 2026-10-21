package com.eventflow.eventflow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private final long orderId;
    private final long customerId;
    private final BigDecimal totalAmount;
    private final String status;
    private final LocalDateTime createdAt;

    public Order(long orderId, long customerId, BigDecimal totalAmount, String status, LocalDateTime createdAt) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getOrderId() {
        return orderId;
    }

    public long getCustomerId() {
        return customerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
