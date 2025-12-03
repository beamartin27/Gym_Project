package com.gym.service;

import com.gym.domain.Booking;
import com.gym.domain.ClassSchedule;
import java.util.List;

public interface BookingService {
    boolean bookClass(int userId, int scheduleId);
    boolean cancelBooking(int bookingId, int userId);

    Booking getBookingById(int bookingId);
    List<Booking> getUserBookings(int userId);
    List<Booking> getScheduleBookings(int scheduleId);

    boolean isScheduleAvailable(int scheduleId);
    boolean hasUserBooked(int userId, int scheduleId);

    boolean markAttended(int bookingId);
}