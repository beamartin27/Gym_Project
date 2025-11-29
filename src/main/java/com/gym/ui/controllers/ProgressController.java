package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.FitnessProgress;
import com.gym.domain.User;
import com.gym.service.ProgressService;
import com.gym.utils.SceneManager;
import com.gym.ui.utils.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProgressController {

    @FXML private TableView<FitnessProgress> progressTable;
    @FXML private TableColumn<FitnessProgress, String>  categoryColumn;
    @FXML private TableColumn<FitnessProgress, Number>  pointsColumn;
    @FXML private TableColumn<FitnessProgress, String>  updatedColumn;
    @FXML private Label totalPointsLabel;
    @FXML private Label messageLabel;

    private ProgressService progressService;

    private final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private void initialize() {
        progressService = AppConfig.getProgressService();

        categoryColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getCategory()));

        pointsColumn.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().getTotalPoints()));

        updatedColumn.setCellValueFactory(c -> {
            if (c.getValue().getLastUpdated() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(
                    c.getValue().getLastUpdated().format(DATE_FORMATTER)
            );
        });

        loadProgress();
    }

    private void loadProgress() {
        messageLabel.setText("");
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            messageLabel.setText("You must be logged in.");
            progressTable.setItems(FXCollections.emptyObservableList());
            totalPointsLabel.setText("0");
            return;
        }

        // make sure baseline categories exist
        progressService.initializeUserProgress(current.getUserId());

        List<FitnessProgress> list =
                progressService.getAllUserProgress(current.getUserId());

        progressTable.setItems(FXCollections.observableArrayList(list));

        int total = list.stream().mapToInt(FitnessProgress::getTotalPoints).sum();
        totalPointsLabel.setText(String.valueOf(total));
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/member-dashboard.fxml", "Member dashboard");
    }
}
