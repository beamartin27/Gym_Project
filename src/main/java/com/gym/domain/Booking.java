package com.gym.domain;

import java.time.LocalDateTime;

public class Booking {
    private int bookingId;
    private int userId;
    private int scheduleId;
    private LocalDateTime bookingDate;
    private String status; // "CONFIRMED", "CANCELLED", "ATTENDED"

    // Constructor for new booking
    public Booking(int userId, int scheduleId, String status) {
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.status = status;
        this.bookingDate = LocalDateTime.now();
    }

    // Constructor for existing booking from database
    public Booking(int bookingId, int userId, int scheduleId,
                   LocalDateTime bookingDate, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.scheduleId = scheduleId;
        this.bookingDate = bookingDate;
        this.status = status;
    }

    // Getters
    public int getBookingId() { return bookingId; }
    public int getUserId() { return userId; }
    public int getScheduleId() { return scheduleId; }
    public LocalDateTime getBookingDate() { return bookingDate; }
    public String getStatus() { return status; }

    // Setters
    public void setBookingId(int bookingId) { this.bookingId = bookingId; }
    public void setStatus(String status) { this.status = status; }

    public boolean isConfirmed() {
        return "CONFIRMED".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    public boolean isAttended() { return "ATTENDED".equalsIgnoreCase(status);}

    public void cancel() {
        this.status = "CANCELLED";
    }

    public void confirm() {
        this.status = "CONFIRMED";
    }

    public void markAttended() { this.status = "ATTENDED"; }

    @Override
    public String toString() {
        return "Booking{" +
                "bookingId=" + bookingId +
                ", userId=" + userId +
                ", scheduleId=" + scheduleId +
                ", bookingDate=" + bookingDate +
                ", status='" + status + '\'' +
                '}';
    }
}