package com.eventflow.eventflow.services;

import com.eventflow.eventflow.model.Ticket;
import com.eventflow.eventflow.repository.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TicketService {
    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Ticket> getAllTickets() {
        return ticketRepository.findAll();
    }

    public Optional<Ticket> getTicketById(long ticketId) {
        return ticketRepository.findById(ticketId);
    }

    public List<Ticket> getTicketsByEventId(long eventId) {
        return ticketRepository.findByEventId(eventId);
    }
}
