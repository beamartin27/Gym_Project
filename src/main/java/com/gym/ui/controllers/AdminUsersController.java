package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.User;
import com.gym.repository.UserRepository;
import com.gym.service.AuthService;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.stream.Collectors;

public class AdminUsersController {

    // TABLE
    @FXML
    private TableView<User> usersTable;

    @FXML
    private TableColumn<User, Integer> idColumn;

    @FXML
    private TableColumn<User, String> usernameColumn;

    @FXML
    private TableColumn<User, String> emailColumn;

    @FXML
    private TableColumn<User, String> roleColumn;

    @FXML
    private TableColumn<User, String> createdAtColumn;

    // FORM FIELDS
    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField emailField;

    @FXML
    private ComboBox<String> roleComboBox;

    private final AuthService authService = AppConfig.getAuthService();
    private final UserRepository userRepository = AppConfig.getUserRepository();

    @FXML
    public void initialize() {
        // Table columns
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Roles admin can create
        roleComboBox.setItems(FXCollections.observableArrayList("MEMBER", "TRAINER"));

        loadUsers();
    }

    private void loadUsers() {
        List<User> users = userRepository.findAll().stream()
                // don’t let this screen touch admins
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                .collect(Collectors.toList());

        usersTable.setItems(FXCollections.observableArrayList(users));
    }

    private void clearForm() {
        usernameField.clear();
        passwordField.clear();
        emailField.clear();
        roleComboBox.getSelectionModel().clearSelection();
    }

    // === BUTTON HANDLERS ===

    @FXML
    private void onAddUserClicked() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String email = emailField.getText().trim();
        String role = roleComboBox.getValue();

        // Basic required fields
        if (username.isEmpty() || password.isEmpty() || email.isEmpty() || role == null) {
            showAlert("Validation error", "Username, password, email and role are required.");
            return;
        }

        // 🔴 NEW: password length validation (so we don't rely only on console)
        if (password.length() < 6) {
            showAlert("Validation error", "Password must be at least 6 characters long.");
            return;
        }

        boolean ok = authService.register(username, password, email, role);
        if (!ok) {
            // There can be other reasons (duplicate username/email, etc.)
            showAlert("Error", "Could not create user. " +
                    "Check that the username and email are not already in use and that the data is valid.");
            return;
        }

        clearForm();
        loadUsers();
    }

    @FXML
    private void onDeleteUserClicked() {
        User selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Select a user to delete.");
            return;
        }

        if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
            showAlert("Not allowed", "You cannot delete admin users from here.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete user '" + selected.getUsername() + "' ?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean ok = userRepository.delete(selected.getUserId());
        if (!ok) {
            showAlert("Error", "Could not delete user.");
            return;
        }

        loadUsers();
    }

    @FXML
    private void onClearFormClicked() {
        clearForm();
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
