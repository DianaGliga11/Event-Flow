package com.eventflow.eventflow.puchase;

//class that only transports data
public record PurchaseRequest(
        long customerId,
        long eventId,
        long seatId
) {
    public PurchaseRequest{
        if(customerId <= 0){
            throw new IllegalArgumentException("customerId must be greater than 0");
        }
        if(eventId <= 0){
            throw new IllegalArgumentException("eventId must be greater than 0");
        }
        if(seatId <= 0){
            throw new IllegalArgumentException("seatId must be greater than 0");
        }
    }
}
