package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.Booking;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.domain.User;
import com.gym.service.AuthService;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.utils.SceneManager;
import com.gym.ui.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDateTime;
import java.util.List;

public class MemberDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label nextClassLabel;

    private final AuthService authService = AppConfig.getAuthService();
    private final BookingService bookingService = AppConfig.getBookingService();
    private final ClassService classService = AppConfig.getClassService();

    @FXML
    private void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Welcome, " + current.getUsername());
            updateNextClassLabel(current);
        } else {
            welcomeLabel.setText("Member dashboard");
            nextClassLabel.setText("No upcoming classes (yet)");
        }
    }

    /**
     * Finds the next upcoming CONFIRMED booking for this user
     * (today or future) and shows it. If none, shows the default text.
     */
    private void updateNextClassLabel(User current) {
        if (current == null) {
            nextClassLabel.setText("No upcoming classes (yet)");
            return;
        }

        List<Booking> bookings = bookingService.getUserBookings(current.getUserId());
        if (bookings == null || bookings.isEmpty()) {
            nextClassLabel.setText("No upcoming classes (yet)");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        ClassSchedule best = null;

        for (Booking b : bookings) {
            // only confirmed bookings
            if (!b.isConfirmed()) {
                continue;
            }

            ClassSchedule schedule = classService.getScheduleById(b.getScheduleId());
            if (schedule == null) {
                continue;
            }

            LocalDateTime start = LocalDateTime.of(
                    schedule.getScheduledDate(),
                    schedule.getStartTime()
            );

            // only future or ongoing classes
            if (start.isBefore(now)) {
                continue;
            }

            if (best == null) {
                best = schedule;
            } else {
                LocalDateTime bestStart = LocalDateTime.of(
                        best.getScheduledDate(),
                        best.getStartTime()
                );
                if (start.isBefore(bestStart)) {
                    best = schedule;
                }
            }
        }

        if (best == null) {
            nextClassLabel.setText("No upcoming classes (yet)");
            return;
        }

        GymClass gymClass = classService.getClassById(best.getClassId());
        String className = (gymClass != null)
                ? gymClass.getClassName()
                : "Class #" + best.getClassId();

        String dateStr = best.getScheduledDate().toString(); // e.g. 2025-11-30
        String timeRange = best.getStartTime() + " - " + best.getEndTime();

        nextClassLabel.setText(className + " · " + dateStr + " · " + timeRange);
    }

    @FXML
    private void onBookClassClicked() {
        SceneManager.switchTo("/views/booking.fxml", "Book a class");
    }

    @FXML
    private void onMyBookingsClicked() {
        SceneManager.switchTo("/views/my-bookings.fxml", "My bookings");
    }

    @FXML
    private void onProgressClicked() {
        SceneManager.switchTo("/views/progress.fxml", "My progress");
    }

    @FXML
    private void onLogoutClicked() {
        authService.logout();
        SessionManager.clear();
        SceneManager.switchTo("/views/login.fxml", "Gym Login");
    }
}
