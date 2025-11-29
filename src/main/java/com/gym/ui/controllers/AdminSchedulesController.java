package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.ClassSchedule;
import com.gym.repository.ClassRepository;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalTime;

public class AdminSchedulesController {

    @FXML
    private TableView<ClassSchedule> schedulesTable;

    @FXML
    private TableColumn<ClassSchedule, Integer> idColumn;

    @FXML
    private TableColumn<ClassSchedule, Integer> classIdColumn;

    @FXML
    private TableColumn<ClassSchedule, LocalDate> dateColumn;

    @FXML
    private TableColumn<ClassSchedule, LocalTime> startTimeColumn;

    @FXML
    private TableColumn<ClassSchedule, LocalTime> endTimeColumn;

    @FXML
    private TableColumn<ClassSchedule, Integer> spotsColumn;

    private final ClassRepository classRepository = AppConfig.getClassRepository();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("scheduleId"));
        classIdColumn.setCellValueFactory(new PropertyValueFactory<>("classId"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("scheduledDate"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        spotsColumn.setCellValueFactory(new PropertyValueFactory<>("availableSpots"));

        schedulesTable.setItems(
                FXCollections.observableArrayList(classRepository.findAllSchedules())
        );
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }
}
