package com.gym.repository.sqlite;

import com.gym.domain.Booking;
import com.gym.repository.BookingRepository;
import com.gym.repository.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SqliteBookingRepository implements BookingRepository {

    private final DatabaseManager dbManager;

    public SqliteBookingRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public boolean save(Booking booking) {
        String sql = "INSERT INTO bookings (user_id, schedule_id, booking_date, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, booking.getUserId());
            pstmt.setInt(2, booking.getScheduleId());
            pstmt.setString(3, booking.getBookingDate().format(formatter));
            pstmt.setString(4, booking.getStatus());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        booking.setBookingId(rs.getInt(1));
                    }
                }
                System.out.println("Booking saved: ID " + booking.getBookingId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error saving booking: " + e.getMessage());
        }
        return false;
    }

    @Override
    public Booking findById(int bookingId) {
        String sql = "SELECT * FROM bookings WHERE booking_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookingId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractBookingFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding booking: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Booking> findByUserId(int userId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE user_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bookings.add(extractBookingFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error finding bookings: " + e.getMessage());
        }
        return bookings;
    }

    @Override
    public List<Booking> findByScheduleId(int scheduleId) {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings WHERE schedule_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, scheduleId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                bookings.add(extractBookingFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error finding bookings: " + e.getMessage());
        }
        return bookings;
    }

    @Override
    public List<Booking> findAll() {
        List<Booking> bookings = new ArrayList<>();
        String sql = "SELECT * FROM bookings";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bookings.add(extractBookingFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting all bookings: " + e.getMessage());
        }
        return bookings;
    }

    @Override
    public boolean update(Booking booking) {
        String sql = "UPDATE bookings SET status = ? WHERE booking_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, booking.getStatus());
            pstmt.setInt(2, booking.getBookingId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Booking updated");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating booking: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean delete(int bookingId) {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bookingId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Booking deleted");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting booking: " + e.getMessage());
        }
        return false;
    }

    private Booking extractBookingFromResultSet(ResultSet rs) throws SQLException {
        LocalDateTime bookingDate = LocalDateTime.parse(
                rs.getString("booking_date"),
                formatter
        );

        return new Booking(
                rs.getInt("booking_id"),
                rs.getInt("user_id"),
                rs.getInt("schedule_id"),
                bookingDate,
                rs.getString("status")
        );
    }
}