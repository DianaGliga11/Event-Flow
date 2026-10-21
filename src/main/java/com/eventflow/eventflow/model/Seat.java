package com.eventflow.eventflow.model;

import java.math.BigDecimal;

public class Seat {
    private final long seatId;
    private final long eventId;
    private final int seatNumber;
    private final BigDecimal price;
    private final String status;

    public Seat(long seatId, long eventId, int seatNumber, BigDecimal price, String status) {
        this.seatId = seatId;
        this.eventId = eventId;
        this.seatNumber = seatNumber;
        this.price = price;
        this.status = status;
    }

    public long getSeatId() {
        return seatId;
    }

    public long getEventId() {
        return eventId;
    }

    public int getSeatNumber() {
        return seatNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}
