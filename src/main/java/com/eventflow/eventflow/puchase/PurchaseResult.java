package com.eventflow.eventflow.puchase;

import java.math.BigDecimal;

//class that only transport data
public record PurchaseResult(
        long orderId,
        long paymentId,
        long ticketId,
        BigDecimal amount,
        String status
) {
}
