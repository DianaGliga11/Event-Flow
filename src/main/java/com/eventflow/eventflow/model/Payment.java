package com.eventflow.eventflow.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private final long paymentId;
    private final long orderId;
    private final BigDecimal amount;
    private final String status;
    private final LocalDateTime createdAt;

    public Payment(long paymentId, long orderId, BigDecimal amount, String status, LocalDateTime createdAt) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getPaymentId() {
        return paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
