package com.eventflow.eventflow.puchase;

import com.eventflow.eventflow.database.MySqlConnection;
import com.eventflow.eventflow.database.PostgresConnection;
import com.eventflow.eventflow.transaction.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class TransactionCoordinator {
    private final PostgresConnection postgresConnection;
    private final MySqlConnection mySqlConnection;

    private final TransactionManager transactionManager;
    private final LockManager lockManager;
    private final UndoLog undoLog;

    public TransactionCoordinator(
            PostgresConnection postgresConnection,
            MySqlConnection mySqlConnection,
            TransactionManager transactionManager,
            LockManager lockManager,
            UndoLog undoLog
    ) {
        this.postgresConnection = postgresConnection;
        this.mySqlConnection = mySqlConnection;
        this.transactionManager = transactionManager;
        this.lockManager = lockManager;
        this.undoLog = undoLog;
    }

    public PurchaseResult purchaseTicket(PurchaseRequest request) {
        //start application-level transaction
        Transaction transaction = transactionManager.beginTransaction();
        long transactionId = transaction.getId();

        try {
            //Acquire application-level locks
            lockManager.acquireLock(transaction, new LockResource("events", String.valueOf(request.eventId())), LockType.READ);

            lockManager.acquireLock(transaction, new LockResource("customers", String.valueOf(request.customerId())), LockType.READ);

            lockManager.acquireLock(transaction, new LockResource("seats", String.valueOf(request.seatId())), LockType.WRITE);

            //Open connections to both databases
            try (
                    Connection postgres = postgresConnection.getConnection();
                    Connection sql = mySqlConnection.getConnection()
            ) {
                postgres.setAutoCommit(true);
                sql.setAutoCommit(true);

                //Validate event, customer and seat
                validateEvent(postgres, request.eventId());
                validateCustomer(sql, request.customerId());

                BigDecimal amount = getAvailableSeatPrice(postgres, request.eventId(), request.seatId());

                //Create order in MySql
                long orderId = createOrder(sql, request.customerId(), amount);

                undoLog.addOperation(transactionId, () ->
                        deleteById(mySqlConnection, "DELETE FROM ticket_orders WHERE order_id = ?", orderId));

                //Register payment in MySql
                long paymentId = createPayment(sql, orderId, amount);

                undoLog.addOperation(transactionId, () ->
                        deleteById(mySqlConnection, "DELETE FROM payments WHERE payment_id = ?", paymentId));

                //confirm order in MySql
                confirmOrder(sql, orderId);

                //Mark seat as SOLD in PostgreSql
                updateSeatStatus(postgres, request.seatId());

                undoLog.addOperation(transactionId, () ->
                        restoreSeat(postgresConnection, request.seatId()));

                //create ticket in PostgreSql
                long ticketId = createTicket(postgres, request.eventId(), request.seatId(), orderId);

                undoLog.addOperation(transactionId, () ->
                        deleteById(postgresConnection, "DELETE FROM tickets WHERE ticket_id = ?", ticketId));

                //Commit application-level transaction
                transactionManager.commitTransaction(transactionId);

                System.out.println("[PURCHASE] Ticket purchased successfully. Order ID: " + orderId);

                return new PurchaseResult(orderId, paymentId, ticketId, amount, "confirmed");
            }
        } catch (Exception e) {
            System.err.println("[PURCHASE] Transaction failed: " + e.getMessage());
            if (transaction.isActive()) {
                try {
                    transactionManager.abortTransaction(transactionId);
                } catch (Exception rollbackException) {
                    e.addSuppressed(rollbackException);
                    System.err.println("[PURCHASE] Rollback failed: " + rollbackException.getMessage());
                }
            }

            throw new IllegalStateException("Ticket purchase failed", e);
        }
    }

    private void validateEvent(Connection connection, long eventId) throws SQLException {
        String sql = """
                SELECT status
                FROM events
                WHERE event_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Event not found");
                }

                if (!"ACTIVE".equals(resultSet.getString("status"))) {
                    throw new IllegalStateException("Event not active");
                }
            }
        }
    }

    private void validateCustomer(Connection connection, long customerId) throws SQLException {
        String sql = """
                SELECT customer_id
                FROM customers
                WHERE customer_id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Customer not found");
                }
            }
        }
    }

    private BigDecimal getAvailableSeatPrice(Connection connection, long eventId, long seatId) throws SQLException {
        String sql = """
                SELECT price
                FROM seats
                WHERE seat_id = ?
                AND event_id = ?
                AND status = 'AVAILABLE'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seatId);
            statement.setLong(2, eventId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    throw new IllegalArgumentException("Seat is not available for this event");
                }

                return resultSet.getBigDecimal("price");
            }
        }
    }

    private long createOrder(
            Connection connection, long customerId, BigDecimal seatId
    ) throws SQLException {
        String sql = """
                INSERT INTO ticket_orders
                (customer_id, total_amount, status)
                VALUES(?, ?, 'pending')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, customerId);
            statement.setBigDecimal(2, seatId);

            statement.executeUpdate();

            return getGeneratedId(statement);
        }
    }

    private long createPayment(
            Connection connection, long order_id, BigDecimal amount) throws SQLException {
        String sql = """
                INSERT INTO payments
                (order_id, amount, status)
                VALUES(?, ?, 'completed')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, order_id);
            statement.setBigDecimal(2, amount);

            statement.executeUpdate();

            return getGeneratedId(statement);
        }
    }

    private void confirmOrder(Connection connection, long orderId) throws SQLException {
        String sql = """
                UPDATE ticket_orders
                SET status = 'CONFIRMED'
                WHERE order_id = ?
                and status = 'pending'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not confirm order");
            }
        }
    }

    private void updateSeatStatus(Connection connection, long seatId) throws SQLException {
        String sql = """
                UPDATE seats
                SET status = 'SOLD'
                WHERE seat_id = ?
                and staus = 'AVAILABLE'
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seatId);

            if (statement.executeUpdate() != 1) {
                throw new SQLException("Could not reserve seat");
            }
        }
    }

    private long createTicket(Connection connection, long eventId, long seatId, long orderId) throws SQLException {
        String sql = """
                INSERT INTO tickets
                (event_id, seat_id, order_id, status)
                VALUES(?, ?, ?, 'ACTIVE')
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, eventId);
            statement.setLong(2, seatId);
            statement.setLong(3, orderId);

            statement.executeUpdate();

            return getGeneratedId(statement);
        }
    }

    private long getGeneratedId(PreparedStatement statement) throws SQLException {
        try (ResultSet keys = statement.getGeneratedKeys()) {
            if (keys.next()) {
                return keys.getLong(1);
            }
        }
        throw new SQLException("Could not retrieve generated ID");
    }

    private void deleteById(Object connectionProvider, String sql, long id) throws Exception {
        Connection connection;
        if (connectionProvider instanceof MySqlConnection provider) {
            connection = provider.getConnection();
        } else if (connectionProvider instanceof PostgresConnection provider) {
            connection = provider.getConnection();
        } else {
            throw new IllegalArgumentException("Unknown connection provider");
        }

        try (connection;
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            statement.executeUpdate();
        }
    }

    private void restoreSeat(PostgresConnection provider, long seatId) throws SQLException {
        String sql = """
                UPDATE seats
                SET status = 'AVAILABLE'
                WHERE seat_id = ?
                """;

        try (Connection connection = provider.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, seatId);
            statement.executeUpdate();
        }
    }
}
