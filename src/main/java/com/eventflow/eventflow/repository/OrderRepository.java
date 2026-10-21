package com.eventflow.eventflow.repository;

import com.eventflow.eventflow.database.MySqlConnection;
import com.eventflow.eventflow.model.Order;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderRepository {
    private final MySqlConnection mySqlConnection;

    public OrderRepository(MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    public List<Order> findAll(){
        String sql = """
                select order_id, customer_id, total_amount, status, created_at
                from orders
                order by created_at desc
                """;

        List<Order> orders = new ArrayList<>();

        try(Connection connection = mySqlConnection.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet result  = statement.executeQuery()){
            while(result.next()){
                orders.add(mapOrder(result));
            }
            return orders;
        }catch(SQLException e){
            throw new IllegalStateException("Could not retrieve orders from MySQL ", e);
        }
    }

    public Optional<Order> findById(long orderId){
        String sql = """
                select order_id, customer_id, total_amount, status, created_at
                from orders
                where order_id = ?
                """;

        try(Connection connection = mySqlConnection.getConnection();
        PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setLong(1, orderId);

            try(ResultSet result = statement.executeQuery()){
                if(result.next()){
                    return Optional.of(mapOrder(result));
                }
                return Optional.empty();
            }
        }catch (SQLException e){
            throw new IllegalStateException("Could not retrieve order  " + orderId, e);
        }
    }

    private Order mapOrder(ResultSet result) throws SQLException{
        Timestamp timestamp = result.getTimestamp("created_at");

        return new Order(
                result.getLong("order_id"),
                result.getLong("customer_id"),
                result.getBigDecimal("total_amount"),
                result.getString("status"),
                timestamp.toLocalDateTime()
        );
    }
}
