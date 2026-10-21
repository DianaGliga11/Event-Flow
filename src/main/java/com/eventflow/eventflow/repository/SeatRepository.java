package com.eventflow.eventflow.repository;

import com.eventflow.eventflow.database.PostgresConnection;
import com.eventflow.eventflow.model.Seat;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class SeatRepository {
    private final PostgresConnection postgresConnection;

    public SeatRepository(PostgresConnection postgresConnection) {
        this.postgresConnection = postgresConnection;
    }

    public List<Seat> findByEventId(long eventId){
        String sql = """
                select seat_id, event_id, seat_number, price, status
                from seats
                where event_id = ?
                order by seat_number
                """;
        List<Seat> seats = new ArrayList<>();

        try(Connection connection = postgresConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setLong(1, eventId);

            try(ResultSet result = statement.executeQuery()){
                while(result.next()){
                    seats.add(mapSeat(result));
                }
            }
            return seats;
        }catch (SQLException e){
            throw new IllegalStateException("Could not retrieve seats for event " + eventId, e);
        }
    }

    public List<Seat> findAvailableByEventId(long eventId){
        String sql = """
                select seat_id, event_id, seat_number, price, status
                from seats
                where event_id = ?
                    and status = 'AVAILABLE'
                order by seat_number
                """;

        List<Seat> seats = new ArrayList<>();

        try(Connection connection = postgresConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setLong(1, eventId);

            try(ResultSet result = statement.executeQuery()){
                while(result.next()){
                    seats.add(mapSeat(result));
                }
            }
            return seats;
        }catch(SQLException e){
            throw new IllegalStateException("Could not retrieve seats for event " + eventId, e);
        }
    }

    public Optional<Seat> findById(long seatId){
        String sql = """
                select seat_id, event_id, seat_number, price, status
                from seats
                where seat_id = ?
                """;

        try(Connection connection = postgresConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setLong(1, seatId);

            try(ResultSet result = statement.executeQuery()){
                while(result.next()){
                    return Optional.of(mapSeat(result));
                }
                return Optional.empty();
            }
        }catch (SQLException e){
            throw new IllegalStateException("Could not retrieve seat  " + seatId, e);
        }
    }

    private Seat mapSeat(ResultSet result) throws SQLException {
        BigDecimal price = result.getBigDecimal("price");

        return new Seat(
                result.getLong("seat_id"),
                result.getLong("event_id"),
                result.getInt("seat_number"),
                price,
                result.getString("status")
        );
    }
}
