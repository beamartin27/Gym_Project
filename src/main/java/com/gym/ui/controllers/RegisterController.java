package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.service.AuthService;
import com.gym.utils.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class RegisterController {

    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;

    private final AuthService authService = AppConfig.getAuthService();

    @FXML
    private void onRegisterClicked() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText().trim();
        String confirm = confirmPasswordField.getText().trim();
        String email = emailField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            errorLabel.setText("All fields are required.");
            return;
        }

        if (!password.equals(confirm)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        // Call your existing registration method
        boolean ok = authService.register(username, password, email, "MEMBER");

        if (ok) {
            errorLabel.setStyle("-fx-text-fill: green;");
            errorLabel.setText("Registration successful! Please log in.");
        } else {
            errorLabel.setStyle("-fx-text-fill: red;");
            errorLabel.setText("Registration failed. Check input.");
        }
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/login.fxml", "Login");
    }
}
