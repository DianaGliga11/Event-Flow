package com.eventflow.eventflow.model;

import java.time.LocalDateTime;

public class Ticket {
    private final long ticketId;
    private final long eventId;
    private final long seatId;
    private final long orderId;
    private final String status;
    private final LocalDateTime timestamp;

    public Ticket(long ticketId, long eventId, long seatId, long orderId, String status, LocalDateTime timestamp) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.seatId = seatId;
        this.orderId = orderId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public long getTicketId() {
        return ticketId;
    }

    public long getEventId() {
        return eventId;
    }

    public long getSeatId() {
        return seatId;
    }

    public long getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
