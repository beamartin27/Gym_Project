package com.gym.repository.sqlite;

import com.gym.repository.DatabaseManager;

import java.sql.*;

public class SqliteDatabaseManager implements DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:gym_database.db";

    @Override
    public Connection getConnection() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL);
            return conn;
        } catch (SQLException e) {
            System.err.println("Connection failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    public void initializeDatabase() {
        System.out.println("Initializing database...\n");
        createUsersTable();
        createClassesTable();
        createClassScheduleTable();
        createBookingsTable();
        createFitnessProgressTable();
        System.out.println("\nAll tables have been created");
    }

    private void createUsersTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                role TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeUpdate(sql, "users");
    }

    private void createClassesTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS classes (
                class_id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_name TEXT NOT NULL,
                instructor_name TEXT NOT NULL,
                description TEXT,
                capacity INTEGER NOT NULL,
                duration_minutes INTEGER NOT NULL,
                class_type TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;
        executeUpdate(sql, "classes");
    }

    private void createClassScheduleTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS class_schedule (
                schedule_id INTEGER PRIMARY KEY AUTOINCREMENT,
                class_id INTEGER NOT NULL,
                scheduled_date TEXT NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                available_spots INTEGER NOT NULL,
                FOREIGN KEY (class_id) REFERENCES classes(class_id)
            )
            """;
        executeUpdate(sql, "class_schedule");
    }

    private void createBookingsTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                schedule_id INTEGER NOT NULL,
                booking_date TEXT DEFAULT CURRENT_TIMESTAMP,
                status TEXT DEFAULT 'CONFIRMED',
                FOREIGN KEY (user_id) REFERENCES users(user_id),
                FOREIGN KEY (schedule_id) REFERENCES class_schedule(schedule_id)
            )
            """;
        executeUpdate(sql, "bookings");
    }

    private void createFitnessProgressTable() {
        String sql = """
        CREATE TABLE IF NOT EXISTS fitness_progress (
            progress_id INTEGER PRIMARY KEY AUTOINCREMENT,
            user_id INTEGER NOT NULL,
            category TEXT NOT NULL,
            total_points INTEGER DEFAULT 0,
            last_updated TEXT NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(user_id),
            UNIQUE(user_id, category)
        )
        """;
        executeUpdate(sql, "fitness_progress");
    }

    private void executeUpdate(String sql, String tableName) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            System.out.println("Created table: " + tableName);
        } catch (SQLException e) {
            System.err.println("Error creating " + tableName + ": " + e.getMessage());
        }
    }
    public static void closeConnection(Connection conn) {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}