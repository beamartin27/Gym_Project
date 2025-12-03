package com.gym.repository;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseManager {
    Connection getConnection() throws SQLException;
    void initializeDatabase();
}
