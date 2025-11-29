package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.ClassSchedule;
import com.gym.domain.User;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.ui.utils.SessionManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class BookingController {

    @FXML
    private DatePicker datePicker;

    @FXML
    private TableView<ClassSchedule> scheduleTable;

    @FXML
    private TableColumn<ClassSchedule, String> timeColumn;

    @FXML
    private TableColumn<ClassSchedule, String> classIdColumn;

    @FXML
    private TableColumn<ClassSchedule, String> dateColumn;

    @FXML
    private TableColumn<ClassSchedule, Number> spotsColumn;

    @FXML
    private Label selectionLabel;

    @FXML
    private Label messageLabel;

    private final ClassService classService = AppConfig.getClassService();
    private final BookingService bookingService = AppConfig.getBookingService();

    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        // Date picker default: today
        datePicker.setValue(LocalDate.now());

        // Configure columns
        timeColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            String text = s.getStartTime().format(timeFmt) + " - " + s.getEndTime().format(timeFmt);
            return new SimpleStringProperty(text);
        });

        classIdColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(String.valueOf(cd.getValue().getClassId())));

        dateColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(cd.getValue().getScheduledDate().format(dateFmt)));

        spotsColumn.setCellValueFactory(cd ->
                new SimpleIntegerProperty(cd.getValue().getAvailableSpots()));

        // When user selects a row
        scheduleTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldV, newV) -> {
                    if (newV == null) {
                        selectionLabel.setText("No class selected");
                    } else {
                        selectionLabel.setText(
                                "Selected: " +
                                        newV.getScheduledDate().format(dateFmt) + " " +
                                        newV.getStartTime().format(timeFmt) +
                                        " (class id " + newV.getClassId() + ")"
                        );
                    }
                });

        loadSchedulesForSelectedDate();
    }

    @FXML
    private void onDateChanged() {
        loadSchedulesForSelectedDate();
    }

    private void loadSchedulesForSelectedDate() {
        messageLabel.setText("");
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
            datePicker.setValue(selectedDate);
        }

        List<ClassSchedule> schedules = classService.getSchedulesByDate(selectedDate);
        scheduleTable.setItems(FXCollections.observableArrayList(schedules));

        if (schedules.isEmpty()) {
            selectionLabel.setText("No classes on " + selectedDate.format(dateFmt));
        } else {
            selectionLabel.setText("Select a class from the list");
        }
    }

    @FXML
    private void onConfirmClicked() {
        messageLabel.setText("");

        ClassSchedule selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            messageLabel.setText("Please select a class first.");
            return;
        }

        User current = SessionManager.getCurrentUser();
        if (current == null) {
            messageLabel.setText("You must be logged in.");
            return;
        }

        boolean ok = bookingService.bookClass(current.getUserId(), selected.getScheduleId());
        if (ok) {
            messageLabel.setText("Class booked! ✅");
            loadSchedulesForSelectedDate();
        } else {
            messageLabel.setText("Could not book this class.");
        }
    }
}
