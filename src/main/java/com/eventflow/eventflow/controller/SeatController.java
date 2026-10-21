package com.eventflow.eventflow.controller;


import com.eventflow.eventflow.model.Seat;
import com.eventflow.eventflow.services.SeatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SeatController {
    private final SeatService seatService;

    public SeatController(SeatService seatService) {
        this.seatService = seatService;
    }

    @GetMapping("/events/{eventId}/seats")
    public List<Seat> getSeatByEventId(@PathVariable long eventId) {
        return seatService.getSeatsByEventId(eventId);
    }

    @GetMapping("/events/{eventId}/setas/available")
    public List<Seat> getAvailableSeatsByEventId(@PathVariable long eventId) {
        return seatService.getAvailableSeats(eventId);
    }

    @GetMapping("/setas/{seatId}")
    public ResponseEntity<Seat> getSeatById(@PathVariable long seatId) {
        return seatService.getSeatById(seatId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
