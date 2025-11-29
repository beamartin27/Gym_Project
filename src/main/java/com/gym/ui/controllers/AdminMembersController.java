package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.User;
import com.gym.repository.UserRepository;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.util.stream.Collectors;

public class AdminMembersController {

    @FXML
    private TableView<User> membersTable;

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

    @FXML
    private Button backButton;

    private final UserRepository userRepository = AppConfig.getUserRepository();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        loadMembers();
    }

    private void loadMembers() {
        List<User> allUsers = userRepository.findAll();
        // Only show members (role MEMBER)
        List<User> members = allUsers.stream()
                .filter(User::isMember)
                .collect(Collectors.toList());

        membersTable.setItems(FXCollections.observableArrayList(members));
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }
}