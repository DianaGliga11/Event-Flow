package com.eventflow.eventflow.repository;

import com.eventflow.eventflow.database.PostgresConnection;
import com.eventflow.eventflow.model.Event;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class EventRepository {
    private final PostgresConnection postgresConnection;

    public EventRepository(PostgresConnection postgresConnection) {
        this.postgresConnection = postgresConnection;
    }

    public List<Event> findAll() {
        String sql = """
                select event_id, name, location, event_date, status
                from events
                order by event_date
                """;
        List<Event> events = new ArrayList<>();
        try (Connection connection = postgresConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {

            while (result.next()) {
                events.add(mapEvent(result));
            }
            return events;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve events from PostgreSQL", e);
        }
    }

    public Optional<Event> findById(long eventId) {
        String sql = """
                select event_id, name, location, event_date, status
                from events
                where event_id = ?
                """;
        try (Connection connection = postgresConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapEvent(result));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve event " + eventId, e);
        }
    }

    private Event mapEvent(ResultSet result) throws SQLException {
        Timestamp timestamp = result.getTimestamp("event_date");

        return new Event(
                result.getLong("event_id"),
                result.getString("name"),
                result.getString("location"),
                timestamp.toLocalDateTime(),
                result.getString("status")
        );
    }
}
