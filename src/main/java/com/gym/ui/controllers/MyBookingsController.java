package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.Booking;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.domain.User;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.ui.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MyBookingsController {

    @FXML
    private TableView<BookingRow> bookingsTable;
    @FXML
    private TableColumn<BookingRow, String> classColumn;
    @FXML
    private TableColumn<BookingRow, String> dateColumn;
    @FXML
    private TableColumn<BookingRow, String> timeColumn;
    @FXML
    private TableColumn<BookingRow, String> statusColumn;
    @FXML
    private TableColumn<BookingRow, Void> actionColumn;

    @FXML
    private Label messageLabel;

    private BookingService bookingService;
    private ClassService classService;

    private final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        bookingService = AppConfig.getBookingService();
        classService = AppConfig.getClassService();

        // Bind columns
        classColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().className()));
        dateColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().date()));
        timeColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().time()));
        statusColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().status()));

        addCancelButtonColumn();

        loadBookings();
    }

    private void loadBookings() {
        User current = SessionManager.getCurrentUser();
        if (current == null) {
            messageLabel.setText("You must be logged in.");
            bookingsTable.setItems(FXCollections.observableArrayList());
            return;
        }

        List<Booking> bookings = bookingService.getUserBookings(current.getUserId());
        List<BookingRow> rows = new ArrayList<>();

        for (Booking b : bookings) {
            ClassSchedule schedule = classService.getScheduleById(b.getScheduleId());
            if (schedule == null) {
                continue;
            }
            GymClass gymClass = classService.getClassById(schedule.getClassId());
            String className = (gymClass != null)
                    ? gymClass.getClassName()
                    : "Class #" + schedule.getClassId();

            String date = schedule.getScheduledDate().format(DATE_FORMATTER);
            String time = schedule.getStartTime().format(TIME_FORMATTER);

            rows.add(new BookingRow(
                    b.getBookingId(),
                    className,
                    date,
                    time,
                    b.getStatus(),
                    b.isConfirmed()
            ));
        }

        bookingsTable.setItems(FXCollections.observableArrayList(rows));
        messageLabel.setText(rows.isEmpty()
                ? "You have no bookings yet."
                : "");
    }

    private void addCancelButtonColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {

            private final Button btn = new Button("Cancel");

            {
                btn.setOnAction(evt -> {
                    BookingRow row = getTableView().getItems().get(getIndex());
                    User current = SessionManager.getCurrentUser();
                    if (current == null) {
                        messageLabel.setText("You must be logged in.");
                        return;
                    }

                    boolean ok = bookingService.cancelBooking(row.bookingId(), current.getUserId());
                    if (ok) {
                        messageLabel.setText("Booking cancelled.");
                        loadBookings();
                    } else {
                        messageLabel.setText("Could not cancel this booking.");
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    BookingRow row = getTableView().getItems().get(getIndex());
                    // Only allow cancel for confirmed bookings
                    btn.setDisable(!row.confirmed());
                    setGraphic(btn);
                }
            }
        });
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/member-dashboard.fxml", "Member dashboard");
    }

    // Simple view-model joining Booking + Schedule + Class
    public record BookingRow(
            int bookingId,
            String className,
            String date,
            String time,
            String status,
            boolean confirmed
    ) { }
}
