package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.domain.User;
import com.gym.domain.Booking;
import com.gym.service.AuthService;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.ui.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import com.gym.ui.controllers.TrainerAttendanceController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TrainerDashboardController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private Label infoLabel;

    @FXML
    private TableView<ScheduleRow> scheduleTable;
    @FXML
    private TableColumn<ScheduleRow, String> dateColumn;
    @FXML
    private TableColumn<ScheduleRow, String> timeColumn;
    @FXML
    private TableColumn<ScheduleRow, String> classColumn;
    @FXML
    private TableColumn<ScheduleRow, Number> bookedColumn;
    @FXML
    private TableColumn<ScheduleRow, Number> capacityColumn;

    private AuthService authService;
    private ClassService classService;
    private BookingService bookingService;

    private final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    private void initialize() {
        authService = AppConfig.getAuthService();
        classService = AppConfig.getClassService();
        bookingService = AppConfig.getBookingService();

        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Welcome, " + current.getUsername() + " (Trainer)");
        } else {
            welcomeLabel.setText("Trainer dashboard");
        }

        dateColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().date()));
        timeColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().time()));
        classColumn.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().className()));
        bookedColumn.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().booked()));
        capacityColumn.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().capacity()));

        loadSchedule();
    }

    private void loadSchedule() {
        // For now: all schedules with available spots.
        // Later we can filter by instructor name if needed.
        List<ClassSchedule> schedules = classService.getAvailableSchedules();
        List<ScheduleRow> rows = new ArrayList<>();

        for (ClassSchedule s : schedules) {
            GymClass gymClass = classService.getClassById(s.getClassId());
            String name = (gymClass != null)
                    ? gymClass.getClassName()
                    : "Class #" + s.getClassId();

            String classType = (gymClass != null)
                    ? gymClass.getClassType()
                    : "UNKNOWN";

            int booked = (int) bookingService.getScheduleBookings(s.getScheduleId())
                    .stream()
                    .filter(Booking::isConfirmed)   // <– only confirmed
                    .count();

            // Fallback: booked + availableSpots (in case gymClass is null).
            int capacity;
            if (gymClass != null) {
                capacity = gymClass.getCapacity();
            } else {
                capacity = booked + s.getAvailableSpots();
            }

            rows.add(new ScheduleRow(
                    s.getScheduleId(),
                    s.getScheduledDate().format(DATE_FMT),
                    s.getStartTime().format(TIME_FMT) + " - " + s.getEndTime().format(TIME_FMT),
                    name,
                    classType,
                    booked,
                    capacity
            ));
        }

        scheduleTable.setItems(FXCollections.observableArrayList(rows));
    }

    @FXML
    private void onLogoutClicked() {
        if (authService != null) {
            authService.logout();
        }
        SessionManager.clear();
        SceneManager.switchTo("/views/login.fxml", "Gym Login");
    }

    // Simple view model for the table
    public record ScheduleRow(
            int scheduleId,
            String date,
            String time,
            String className,
            String classType,
            int booked,
            int capacity
    ) { }

    @FXML
    private void onMarkAttendanceClicked() {
        ScheduleRow selected = scheduleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            infoLabel.setText("Select a class first.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/trainer-attendance.fxml"));
            Parent root = loader.load();

            TrainerAttendanceController controller = loader.getController();
            controller.setContext(
                    selected.scheduleId(),
                    selected.className(),
                    selected.classType()
            );

            Stage stage = new Stage();
            stage.setTitle("Attendance - " + selected.className());
            stage.setScene(new Scene(root));
            stage.initOwner(scheduleTable.getScene().getWindow());
            stage.show();

            infoLabel.setText("");
        } catch (Exception e) {
            e.printStackTrace();
            infoLabel.setText("Could not open attendance window.");
        }
    }

}
