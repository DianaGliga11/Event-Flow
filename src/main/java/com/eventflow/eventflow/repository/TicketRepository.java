package com.eventflow.eventflow.repository;

import com.eventflow.eventflow.database.PostgresConnection;
import com.eventflow.eventflow.model.Ticket;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TicketRepository {
    private final PostgresConnection postgresConnection;

    public TicketRepository(PostgresConnection postgresConnection) {
        this.postgresConnection = postgresConnection;
    }

    public List<Ticket> findAll() {
        String sql = """
                select * from tickets
                order by created_at desc
                """;

        List<Ticket> tickets = new ArrayList<>();

        try (Connection connection = postgresConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql);
             ResultSet result = preparedStatement.executeQuery()) {

            while (result.next()) {
                tickets.add(mapTicket(result));
            }
            return tickets;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve tickets from PostgreSQL ", e);
        }
    }

    public Optional<Ticket> findById(long ticketId) {
        String sql = """
                select ticket_id, event_id, order_id, status, created_at
                from tickets
                where ticket_id = ?
                """;

        try (Connection connection = postgresConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, ticketId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapTicket(result));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve ticket  " + ticketId, e);
        }
    }

    public List<Ticket> findByEventId(long eventId) {
        String sql = """
                select ticket_id, event_id, order_id, status, created_at
                from tickets
                where event_id = ?
                order by created_at desc
                """;

        List<Ticket> tickets = new ArrayList<>();

        try (Connection connection = postgresConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    tickets.add(mapTicket(result));
                }
            }
            return tickets;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve tickets for event  " + eventId, e);
        }
    }

    private Ticket mapTicket(ResultSet result) throws SQLException {
        Timestamp timestamp = result.getTimestamp("created_at");

        return new Ticket(
                result.getLong("ticket_id"),
                result.getLong("event_id"),
                result.getLong("seat_id"),
                result.getLong("order_id"),
                result.getString("status"),
                timestamp.toLocalDateTime()
        );
    }
}
