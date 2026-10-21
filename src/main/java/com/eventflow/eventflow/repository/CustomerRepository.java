package com.eventflow.eventflow.repository;

import com.eventflow.eventflow.database.MySqlConnection;
import com.eventflow.eventflow.model.Customer;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepository {
    private final MySqlConnection mySqlConnection;

    public CustomerRepository(MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    public List<Customer> findAll() {
        String sql = """
                select customer_id, name, email, created_at
                from customers
                order by created_at desc
                """;

        List<Customer> customers = new ArrayList<>();

        try (Connection conn = mySqlConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }

            return customers;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve customers from MySQL", e);
        }
    }

    public Optional<Customer> findById(long customerId) {
        String sql = """
               select customer_id, name, email, created_at
               from customers
               where customer_id = ?
               order by created_at desc
                """;

        try (Connection connection = mySqlConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);

            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return Optional.of(mapCustomer(result));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Could not retrieve customer " + customerId, e);
        }
    }

    private Customer mapCustomer(ResultSet result) throws SQLException {
        Timestamp timestamp = result.getTimestamp("created_at");

        return new Customer(
                result.getLong("customer_id"),
                result.getString("name"),
                result.getString("email"),
                timestamp.toLocalDateTime()
        );
    }
}
