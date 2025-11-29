package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.User;
import com.gym.service.AuthService;
import com.gym.service.BookingService;
import com.gym.utils.SceneManager;
import com.gym.ui.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

public class MemberDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label nextClassLabel;

    private final AuthService authService = AppConfig.getAuthService();
    private final BookingService bookingService = AppConfig.getBookingService();

    @FXML
    private void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Welcome, " + current.getUsername());
        } else {
            welcomeLabel.setText("Member dashboard");
        }

        // TODO: real "next class" using bookingService
        nextClassLabel.setText("No upcoming classes (yet)");
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
