package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.GymClass;
import com.gym.service.ClassService;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminClassesController {

    @FXML
    private TableView<GymClass> classesTable;

    @FXML
    private TableColumn<GymClass, Integer> idColumn;

    @FXML
    private TableColumn<GymClass, String> nameColumn;

    @FXML
    private TableColumn<GymClass, String> instructorColumn;

    @FXML
    private TableColumn<GymClass, String> typeColumn;

    @FXML
    private TableColumn<GymClass, Integer> capacityColumn;

    @FXML
    private TableColumn<GymClass, Integer> durationColumn;

    private final ClassService classService = AppConfig.getClassService();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("classId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        instructorColumn.setCellValueFactory(new PropertyValueFactory<>("instructorName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("classType"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));

        classesTable.setItems(
                FXCollections.observableArrayList(classService.getAllClasses())
        );
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }
}