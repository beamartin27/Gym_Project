package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.Booking;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.domain.User;
import com.gym.repository.BookingRepository;
import com.gym.repository.ClassRepository;
import com.gym.repository.UserRepository;
import com.gym.service.BookingService;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminBookingsController {

    @FXML
    private TableView<BookingView> bookingsTable;

    @FXML
    private TableColumn<BookingView, Integer> bookingIdColumn;

    @FXML
    private TableColumn<BookingView, String> memberColumn;

    @FXML
    private TableColumn<BookingView, String> classColumn;

    @FXML
    private TableColumn<BookingView, String> dateColumn;

    @FXML
    private TableColumn<BookingView, String> timeColumn;

    @FXML
    private TableColumn<BookingView, String> bookingDateColumn;

    @FXML
    private TableColumn<BookingView, String> statusColumn;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    private final BookingRepository bookingRepository = AppConfig.getBookingRepository();
    private final BookingService bookingService = AppConfig.getBookingService();
    private final UserRepository userRepository = AppConfig.getUserRepository();
    private final ClassRepository classRepository = AppConfig.getClassRepository();

    private ObservableList<BookingView> allBookings = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // configure table columns
        bookingIdColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        memberColumn.setCellValueFactory(new PropertyValueFactory<>("memberName"));
        classColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        bookingDateColumn.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // status filter options
        statusFilter.getItems().addAll("All", "CONFIRMED", "CANCELLED");
        statusFilter.setValue("All");

        loadBookings();
    }

    private void loadBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        List<BookingView> viewList = new ArrayList<>();

        for (Booking booking : bookings) {
            User user = userRepository.findById(booking.getUserId());
            String memberName = user != null ? user.getUsername() : "User #" + booking.getUserId();

            ClassSchedule schedule = classRepository.findScheduleById(booking.getScheduleId());
            String className = "Unknown";
            String dateStr = "";
            String timeStr = "";

            if (schedule != null) {
                GymClass gymClass = classRepository.findClassById(schedule.getClassId());
                if (gymClass != null) {
                    className = gymClass.getClassName();
                }
                LocalDate date = schedule.getScheduledDate();
                LocalTime start = schedule.getStartTime();
                LocalTime end = schedule.getEndTime();

                if (date != null) {
                    dateStr = date.toString();
                }
                if (start != null && end != null) {
                    timeStr = start + " - " + end;
                } else if (start != null) {
                    timeStr = start.toString();
                }
            }

            String bookingDateStr = booking.getBookingDate() != null
                    ? booking.getBookingDate().toString()
                    : "";

            BookingView view = new BookingView(
                    booking.getBookingId(),
                    booking.getUserId(),
                    booking.getScheduleId(),
                    memberName,
                    className,
                    dateStr,
                    timeStr,
                    bookingDateStr,
                    booking.getStatus()
            );
            viewList.add(view);
        }

        allBookings.setAll(viewList);
        bookingsTable.setItems(allBookings);
    }

    // ─── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }

    // ─── Search / filter ──────────────────────────────────────────────────────

    @FXML
    private void onSearchClicked() {
        applyFilters();
    }

    @FXML
    private void onClearSearchClicked() {
        searchField.clear();
        statusFilter.setValue("All");
        bookingsTable.setItems(allBookings);
    }

    private void applyFilters() {
        String term = searchField.getText() != null
                ? searchField.getText().trim().toLowerCase()
                : "";
        String status = statusFilter.getValue();

        List<BookingView> filtered = allBookings.stream()
                .filter(view -> {
                    boolean matchesText;
                    if (term.isEmpty()) {
                        matchesText = true;
                    } else {
                        String idStr = Integer.toString(view.getBookingId());
                        matchesText =
                                view.getMemberName().toLowerCase().contains(term) ||
                                        view.getClassName().toLowerCase().contains(term) ||
                                        idStr.contains(term);
                    }

                    boolean matchesStatus;
                    if (status == null || "All".equalsIgnoreCase(status)) {
                        matchesStatus = true;
                    } else {
                        matchesStatus = status.equalsIgnoreCase(view.getStatus());
                    }

                    return matchesText && matchesStatus;
                })
                .collect(Collectors.toList());

        bookingsTable.setItems(FXCollections.observableArrayList(filtered));
    }

    // ─── Cancel booking ───────────────────────────────────────────────────────

    @FXML
    private void onCancelBookingClicked() {
        BookingView selected = bookingsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select a booking to cancel.");
            return;
        }

        if ("CANCELLED".equalsIgnoreCase(selected.getStatus())) {
            showInfo("This booking is already cancelled.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancel booking");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to cancel this booking?");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean ok = bookingService.cancelBooking(selected.getBookingId(), selected.getUserId());
        if (ok) {
            showInfo("Booking cancelled.");
            loadBookings();
            applyFilters(); // keep current filter if any
        } else {
            showError("Could not cancel booking. Try again.");
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ─── View model used in the TableView ─────────────────────────────────────

    public static class BookingView {
        private final int bookingId;
        private final int userId;
        private final int scheduleId;
        private final String memberName;
        private final String className;
        private final String date;
        private final String time;
        private final String bookingDate;
        private final String status;

        public BookingView(int bookingId,
                           int userId,
                           int scheduleId,
                           String memberName,
                           String className,
                           String date,
                           String time,
                           String bookingDate,
                           String status) {
            this.bookingId = bookingId;
            this.userId = userId;
            this.scheduleId = scheduleId;
            this.memberName = memberName;
            this.className = className;
            this.date = date;
            this.time = time;
            this.bookingDate = bookingDate;
            this.status = status;
        }

        public int getBookingId() {
            return bookingId;
        }

        public int getUserId() {
            return userId;
        }

        public int getScheduleId() {
            return scheduleId;
        }

        public String getMemberName() {
            return memberName;
        }

        public String getClassName() {
            return className;
        }

        public String getDate() {
            return date;
        }

        public String getTime() {
            return time;
        }

        public String getBookingDate() {
            return bookingDate;
        }

        public String getStatus() {
            return status;
        }
    }
}
