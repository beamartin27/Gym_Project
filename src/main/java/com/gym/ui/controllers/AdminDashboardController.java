package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.User;
import com.gym.service.AuthService;
import com.gym.ui.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminDashboardController {

    @FXML
    private Label welcomeLabel;

    private final AuthService authService = AppConfig.getAuthService();

    @FXML
    public void initialize() {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Admin dashboard - " + current.getUsername());
        }
    }

    @FXML
    private void onManageClassesClicked() {
        SceneManager.switchTo("/views/admin-classes.fxml", "Manage classes");
    }

    @FXML
    private void onManageSchedulesClicked() {
        SceneManager.switchTo("/views/admin-schedules.fxml", "Manage schedules");
    }

    @FXML
    private void onViewBookingsClicked() {
        SceneManager.switchTo("/views/admin-bookings.fxml", "All bookings");
    }

    @FXML
    private void onViewMembersClicked() {
        // We will use this to MANAGE users (members + trainers)
        SceneManager.switchTo("/views/admin-users.fxml", "Manage users");
    }

    @FXML
    private void onLogoutClicked() {
        authService.logout();
        SessionManager.clear();
        SceneManager.switchTo("/views/login.fxml", "Gym Login");
    }
}