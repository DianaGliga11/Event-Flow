
package com.eventflow.eventflow.database;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class DatabaseConnectionTest implements CommandLineRunner {

    private final PostgresConnection postgresConnection;
    private final MySqlConnection mySqlConnection;

    public DatabaseConnectionTest(
            PostgresConnection postgresConnection,
            MySqlConnection mySqlConnection) {
        this.postgresConnection = postgresConnection;
        this.mySqlConnection = mySqlConnection;
    }

    @Override
    public void run(String... args) {
        testPostgresConnection();
        testMySqlConnection();
    }

    private void testPostgresConnection() {
        try (Connection connection = postgresConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT current_database()")) {

            if (result.next()) {
                System.out.println(
                        "[SUCCESS] PostgreSQL connected to: " + result.getString(1));
            }

        } catch (Exception e) {
            System.err.println("[ERROR] PostgreSQL connection failed: "
                    + e.getMessage());
        }
    }

    private void testMySqlConnection() {
        try (Connection connection = mySqlConnection.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT DATABASE()")) {

            if (result.next()) {
                System.out.println(
                        "[SUCCESS] MySQL connected to: " + result.getString(1));
            }

        } catch (Exception e) {
            System.err.println("[ERROR] MySQL connection failed: "
                    + e.getMessage());
        }
    }
}
