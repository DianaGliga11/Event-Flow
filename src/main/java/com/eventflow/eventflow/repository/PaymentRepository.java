package com.eventflow.eventflow.repository;

import com.eventflow.eventflow.database.MySqlConnection;
import com.eventflow.eventflow.model.Payment;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PaymentRepository {
    private final MySqlConnection mySqlConnection;

    public PaymentRepository(MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    public List<Payment> findAll() {
        String sql = """
                select payment_id, order_id, amount, status, created_at
                from payment
                order by created_at desc
                """;

        List<Payment> payments = new ArrayList<>();

        try (Connection conn = mySqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                payments.add(mapPayment(rs));
            }
            return payments;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve payments from MySQL", e);
        }
    }

    public Optional<Payment> findById(long paymentId) {
        String sql = """
                select payment_id, order_id, amount, status, created_at
                from payment
                where payment_id = ?
                """;

        try (Connection connection = mySqlConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, paymentId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapPayment(result));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve payment " + paymentId, e);
        }
    }

    private Payment mapPayment(ResultSet result) throws SQLException {
        Timestamp timestamp = result.getTimestamp("created_at");

        return new Payment(
                result.getLong("payment_id"),
                result.getLong("order_id"),
                result.getBigDecimal("amount"),
                result.getString("status"),
                timestamp.toLocalDateTime()
        );
    }
}
