package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.User;
import com.gym.service.AuthService;
import com.gym.utils.SceneManager;
import com.gym.ui.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private AuthService authService;

    @FXML
    private void initialize() {
        // This gets executed once FXML loads
        this.authService = AppConfig.getAuthService();
        if (this.authService == null) {
            System.err.println("ERROR: AuthService is null. Did AppMain call AppConfig.init()?");
        }
        passwordField.setOnAction(e -> onLoginClicked());
    }

    @FXML
    private void onRegisterClicked() {
        SceneManager.switchTo("/views/register.fxml", "Register");
    }

    @FXML
    private void onLoginClicked() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        errorLabel.setText("");

        if (authService == null) {
            errorLabel.setText("Internal error: auth not ready");
            return;
        }

        User logged = authService.login(username, password);
        if (logged == null) {
            errorLabel.setText("Invalid username or password");
            return;
        }

        SessionManager.setCurrentUser(logged);

        String role = logged.getRole(); // make sure this is "ADMIN" / "MEMBER" etc.

        if ("ADMIN".equalsIgnoreCase(role)) {
            SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
        } else if ("TRAINER".equalsIgnoreCase(role)) {
            SceneManager.switchTo("/views/trainer-dashboard.fxml", "Trainer dashboard");
        } else {
            SceneManager.switchTo("/views/member-dashboard.fxml", "Member dashboard");
        }
    }
}
