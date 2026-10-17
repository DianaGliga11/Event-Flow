package com.eventflow.eventflow.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class MySqlConnection {
    private final String URL;

    private final String USER;

    private final String PASSWORD;

    private MySqlConnection(
            @Value("${eventflow.datasource.mysql.url}") String url,
            @Value("${eventflow.datasource.mysql.username}") String username,
            @Value("${eventflow.datasource.mysql.password}") String password
    ) {
        this.URL = url;
        this.USER = username;
        this.PASSWORD = password;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
