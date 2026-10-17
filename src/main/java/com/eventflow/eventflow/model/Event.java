package com.eventflow.eventflow.model;

import java.time.LocalDateTime;

public class Event {
    private final long eventId;
    private final String name;
    private final String location;
    private final LocalDateTime eventDate;
    private final String status;

    public Event(long eventId, String name, String location, LocalDateTime eventDate, String status) {
        this.eventId = eventId;
        this.name = name;
        this.location = location;
        this.eventDate = eventDate;
        this.status = status;
    }

    public long getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public String getStatus() {
        return status;
    }
}
