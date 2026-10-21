package com.eventflow.eventflow.services;

import com.eventflow.eventflow.model.Seat;
import com.eventflow.eventflow.repository.SeatRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SeatService {
    private final SeatRepository seatRepository;

    public SeatService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    public List<Seat> getSeatsByEventId(long eventId) {
        return seatRepository.findByEventId(eventId);
    }

    public List<Seat> getAvailableSeats(long eventId) {
        return seatRepository.findAvailableByEventId(eventId);
    }

    public Optional<Seat> getSeatById(long seatId) {
        return seatRepository.findById(seatId);
    }
}
