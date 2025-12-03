package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.Booking;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MyBookingsController {

    private final BookingService bookingService = AppConfig.getBookingService();
    private final ClassService classService = AppConfig.getClassService();

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

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
    private ComboBox<String> statusFilter;

    @FXML
    private Label messageLabel;

    /** master list before filtering */
    private List<BookingRow> allRows = new ArrayList<>();

    @FXML
    private void initialize() {
        classColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().className()));
        dateColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().date()));
        timeColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().time()));
        statusColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().status()));

        // default sort by date/time
        bookingsTable.getSortOrder().add(dateColumn);

        addCancelButtonColumn();

        // status filter options
        statusFilter.getItems().addAll("All", "Confirmed", "Cancelled");
        statusFilter.setValue("All");
        statusFilter.valueProperty().addListener((obs, oldV, newV) -> applyFilter());

        loadBookings();
    }

    private void loadBookings() {
        var current = SessionManager.getCurrentUser();
        if (current == null) {
            messageLabel.setText("You must be logged in.");
            bookingsTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        List<Booking> bookings = bookingService.getUserBookings(current.getUserId());
        List<BookingRow> rows = new ArrayList<>();

        for (Booking b : bookings) {
            ClassSchedule schedule = classService.getScheduleById(b.getScheduleId());
            if (schedule == null) continue;

            GymClass gymClass = classService.getClassById(schedule.getClassId());
            if (gymClass == null) continue;

            String className = gymClass.getClassName();
            String dateStr = DATE_FORMATTER.format(schedule.getScheduledDate());
            String timeStr = TIME_FORMATTER.format(schedule.getStartTime());
            String status = b.getStatus();

            LocalDateTime startDateTime =
                    LocalDateTime.of(schedule.getScheduledDate(), schedule.getStartTime());

            rows.add(new BookingRow(
                    b.getBookingId(),
                    className,
                    dateStr,
                    timeStr,
                    status,
                    b.isConfirmed(),
                    startDateTime
            ));
        }

        // sort by date/time ascending
        rows.sort(Comparator.comparing(BookingRow::startDateTime));

        this.allRows = rows;
        applyFilter();
        messageLabel.setText("");
    }

    private void applyFilter() {
        if (allRows == null) {
            bookingsTable.setItems(FXCollections.emptyObservableList());
            return;
        }

        String filter = statusFilter.getValue();
        List<BookingRow> filtered = allRows;

        if (filter != null && !"All".equalsIgnoreCase(filter)) {
            String wanted = filter.toUpperCase();
            filtered = allRows.stream()
                    .filter(r -> r.status().equalsIgnoreCase(wanted))
                    .toList();
        }

        bookingsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    private boolean canCancel(BookingRow row) {
        // can cancel up to 2 hours before the class starts
        LocalDateTime now = LocalDateTime.now();
        return now.isBefore(row.startDateTime().minusHours(2));
    }

    private void addCancelButtonColumn() {
        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Cancel");

            {
                btn.setOnAction(e -> {
                    BookingRow row = getTableView().getItems().get(getIndex());
                    var current = SessionManager.getCurrentUser();
                    if (current == null) {
                        messageLabel.setText("You must be logged in.");
                        return;
                    }

                    if (!canCancel(row)) {
                        messageLabel.setText("You can only cancel up to 2 hours before the class.");
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
                    return;
                }

                BookingRow row = getTableView().getItems().get(getIndex());
                boolean showButton = row.confirmed() && canCancel(row);

                if (showButton) {
                    btn.setDisable(false);
                    setGraphic(btn);
                } else {
                    // not cancellable (cancelled or too late) → no button
                    setGraphic(null);
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
            boolean confirmed,
            LocalDateTime startDateTime
    ) { }
}
