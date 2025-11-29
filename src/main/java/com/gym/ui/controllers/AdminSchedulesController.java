package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.service.ClassService;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminSchedulesController {

    // Filter + table
    @FXML
    private DatePicker dateFilterPicker;

    @FXML
    private TableView<ClassSchedule> schedulesTable;

    @FXML
    private TableColumn<ClassSchedule, Number> idColumn;

    @FXML
    private TableColumn<ClassSchedule, String> classColumn;

    @FXML
    private TableColumn<ClassSchedule, String> instructorColumn;

    @FXML
    private TableColumn<ClassSchedule, String> dateColumn;

    @FXML
    private TableColumn<ClassSchedule, String> timeColumn;

    @FXML
    private TableColumn<ClassSchedule, Number> spotsColumn;

    // Form to create schedule
    @FXML
    private ComboBox<GymClass> classComboBox;

    @FXML
    private DatePicker scheduleDatePicker;

    @FXML
    private TextField startTimeField;

    @FXML
    private TextField endTimeField;

    @FXML
    private Label capacityLabel;

    private final ClassService classService = AppConfig.getClassService();
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    // For quick lookup of class name / instructor / capacity by id
    private final Map<Integer, GymClass> classById = new HashMap<>();

    @FXML
    public void initialize() {
        // Load all classes once
        List<GymClass> classes = classService.getAllClasses();
        classComboBox.setItems(FXCollections.observableArrayList(classes));

        // Fill lookup map
        for (GymClass gc : classes) {
            classById.put(gc.getClassId(), gc);
        }

        // Show "Class name (Instructor)" in the combo box
        classComboBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(GymClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getClassName() + " (" + item.getInstructorName() + ")");
                }
            }
        });
        classComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(GymClass item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("Select class");
                } else {
                    setText(item.getClassName() + " (" + item.getInstructorName() + ")");
                }
            }
        });

        // When admin selects a class, show its capacity
        classComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                capacityLabel.setText("Capacity / initial spots: " + selected.getCapacity());
            } else {
                capacityLabel.setText("Capacity / initial spots: -");
            }
        });

        // Default date = today
        LocalDate today = LocalDate.now();
        dateFilterPicker.setValue(today);
        scheduleDatePicker.setValue(today);

        // Table columns
        idColumn.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getScheduleId()));

        classColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            GymClass gc = classById.get(s.getClassId());
            String name = (gc != null) ? gc.getClassName() : "Class " + s.getClassId();
            return new SimpleStringProperty(name);
        });

        instructorColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            GymClass gc = classById.get(s.getClassId());
            String instructor = (gc != null) ? gc.getInstructorName() : "-";
            return new SimpleStringProperty(instructor);
        });

        dateColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getScheduledDate().toString()));

        timeColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            String text = s.getStartTime().format(timeFmt) + " - " + s.getEndTime().format(timeFmt);
            return new SimpleStringProperty(text);
        });

        spotsColumn.setCellValueFactory(cd ->
                new SimpleIntegerProperty(cd.getValue().getAvailableSpots()));

        // Load schedules for today
        loadSchedulesForDate(today);
    }

    private void loadSchedulesForDate(LocalDate date) {
        List<ClassSchedule> schedules = classService.getSchedulesByDate(date);
        schedulesTable.setItems(FXCollections.observableArrayList(schedules));
    }

    @FXML
    private void onFilterDateChanged() {
        LocalDate date = dateFilterPicker.getValue();
        if (date != null) {
            loadSchedulesForDate(date);
        }
    }

    @FXML
    private void onAddScheduleClicked() {
        GymClass selectedClass = classComboBox.getValue();
        LocalDate date = scheduleDatePicker.getValue();
        String startText = startTimeField.getText().trim();
        String endText = endTimeField.getText().trim();

        if (selectedClass == null || date == null ||
                startText.isEmpty() || endText.isEmpty()) {
            showAlert("Validation error",
                    "Class, date, start time and end time are required.");
            return;
        }

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startText, timeFmt);
            end = LocalTime.parse(endText, timeFmt);
        } catch (DateTimeParseException e) {
            showAlert("Validation error",
                    "Times must be in HH:mm format, e.g. 09:00");
            return;
        }

        if (!end.isAfter(start)) {
            showAlert("Validation error", "End time must be after start time.");
            return;
        }

        // Do not allow the SAME class (same trainer / classId) at the same date & time
        // but allow same class name with different trainer (different classId).
        List<ClassSchedule> daySchedules = classService.getSchedulesByDate(date);
        for (ClassSchedule existing : daySchedules) {
            if (existing.getClassId() == selectedClass.getClassId()
                    && existing.getStartTime().equals(start)
                    && existing.getEndTime().equals(end)) {
                showAlert(
                        "Validation error",
                        "This class with this trainer is already scheduled at that time."
                );
                return;
            }
        }

        // Available spots at the beginning = class capacity
        int spots = selectedClass.getCapacity();

        ClassSchedule schedule = new ClassSchedule(
                selectedClass.getClassId(),
                date,
                start,
                end,
                spots
        );

        boolean ok = classService.createSchedule(schedule);
        if (!ok) {
            showAlert("Error", "Could not create schedule.");
            return;
        }

        // refresh table
        dateFilterPicker.setValue(date);
        loadSchedulesForDate(date);
        clearForm();
    }

    @FXML
    private void onDeleteScheduleClicked() {
        ClassSchedule selected = schedulesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("No selection", "Select a schedule to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this schedule?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean ok = classService.deleteSchedule(selected.getScheduleId());
        if (!ok) {
            showAlert("Error", "Could not delete schedule.");
            return;
        }

        loadSchedulesForDate(dateFilterPicker.getValue());
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }

    private void clearForm() {
        classComboBox.getSelectionModel().clearSelection();
        startTimeField.clear();
        endTimeField.clear();
        // keep scheduleDatePicker as is
        capacityLabel.setText("Capacity / initial spots: -");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}