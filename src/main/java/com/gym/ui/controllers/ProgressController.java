package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.FitnessProgress;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.service.ProgressService;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.ui.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProgressController {

    @FXML private ProgressBar upperBar;
    @FXML private ProgressBar lowerBar;
    @FXML private ProgressBar armsBar;
    @FXML private ProgressBar cardioBar;

    @FXML private TableView<RecentRow> recentTable;
    @FXML private TableColumn<RecentRow, String> recentClassColumn;
    @FXML private TableColumn<RecentRow, String> recentDateColumn;
    @FXML private TableColumn<RecentRow, String> recentFocusColumn;
    @FXML private TableColumn<RecentRow, Number> recentPointsColumn;

    @FXML private Label messageLabel;

    private ProgressService progressService;
    private BookingService bookingService;
    private ClassService classService;

    private final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");


    @FXML
    private void initialize() {
        progressService = AppConfig.getProgressService();
        bookingService = AppConfig.getBookingService();
        classService = AppConfig.getClassService();

        recentClassColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().className()));
        recentDateColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().date()));
        recentFocusColumn.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().focus()));
        recentPointsColumn.setCellValueFactory(c ->
                new SimpleIntegerProperty(c.getValue().points()));

        loadData();
    }


    private void loadData() {
        var user = SessionManager.getCurrentUser();
        if (user == null) {
            messageLabel.setText("You must be logged in.");
            return;
        }

        progressService.initializeUserProgress(user.getUserId());

        List<FitnessProgress> stats =
                progressService.getAllUserProgress(user.getUserId());

        // Map categories to progress bars
        for (FitnessProgress fp : stats) {
            double pct = fp.getTotalPoints() / 100.0; // scale 0–1

            switch (fp.getCategory().toLowerCase()) {
                case "upper body" -> upperBar.setProgress(pct);
                case "lower body" -> lowerBar.setProgress(pct);
                case "arms" -> armsBar.setProgress(pct);
                case "cardio" -> cardioBar.setProgress(pct);
            }
        }

        loadRecentClasses(user.getUserId());
    }


    private void loadRecentClasses(int userId) {
        List<RecentRow> rows = bookingService.getUserBookings(userId).stream()
                .filter(b -> b.isConfirmed()) // completed = confirmed + past
                .map(b -> {
                    ClassSchedule s = classService.getScheduleById(b.getScheduleId());
                    if (s == null) return null;

                    if (s.getEndTime() == null) return null;

                    LocalDateTime classEnd = LocalDateTime.of(
                            s.getScheduledDate(),
                            s.getEndTime()
                    );

                    if (classEnd.isAfter(LocalDateTime.now())) return null;

                    GymClass gc = classService.getClassById(s.getClassId());
                    if (gc == null) return null;

                    return new RecentRow(
                            gc.getClassName(),
                            DATE_FORMATTER.format(s.getScheduledDate()),
                            gc.getClassType(),
                            10 // Or use real points formula
                    );
                })
                .filter(r -> r != null)
                .toList();

        recentTable.setItems(FXCollections.observableArrayList(rows));
    }


    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/member-dashboard.fxml", "Member dashboard");
    }


    public record RecentRow(
            String className,
            String date,
            String focus,
            int points
    ) {}
}
