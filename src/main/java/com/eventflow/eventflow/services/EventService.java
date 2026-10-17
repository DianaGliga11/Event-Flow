package com.eventflow.eventflow.services;

import com.eventflow.eventflow.model.Event;
import com.eventflow.eventflow.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EventService {
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    public Optional<Event> getEventById(long eventId) {
        return eventRepository.findById(eventId);
    }
}
