
package com.eventflow.eventflow.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class PostgresConnection {

    private final String url;
    private final String username;
    private final String password;

    public PostgresConnection(
            @Value("${eventflow.datasource.postgres.url}") String url,
            @Value("${eventflow.datasource.postgres.username}") String username,
            @Value("${eventflow.datasource.postgres.password}") String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}