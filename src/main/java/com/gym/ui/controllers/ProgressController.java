package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.FitnessProgress;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.service.ProgressService;
import com.gym.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProgressController {

    @FXML private ProgressBar upperBar;
    @FXML private ProgressBar lowerBar;
    @FXML private ProgressBar armsBar;
    @FXML private ProgressBar cardioBar;

    @FXML private Label upperLevelLabel;
    @FXML private Label lowerLevelLabel;
    @FXML private Label armsLevelLabel;
    @FXML private Label cardioLevelLabel;

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

        // ensure rows exist
        progressService.initializeUserProgress(user.getUserId());

        List<FitnessProgress> stats =
                progressService.getAllUserProgress(user.getUserId());

        // aggregate internal categories into the 4 visible areas
        int upperPoints  = 0;
        int lowerPoints  = 0;
        int armsPoints   = 0;
        int cardioPoints = 0;

        for (FitnessProgress fp : stats) {
            String cat = fp.getCategory();
            if (cat == null) continue;

            String category = cat.toUpperCase();
            int points = fp.getTotalPoints();

            switch (category) {
                // overall upper body categories
                case "STRENGTH", "CORE", "FLEXIBILITY" -> upperPoints += points;

                // legs = lower body
                case "LEGS" -> lowerPoints += points;

                // arms only (do NOT also add to upper – to avoid double counting)
                case "ARMS" -> armsPoints += points;

                // cardio + endurance go into the cardio bar
                case "CARDIO", "ENDURANCE" -> cardioPoints += points;

                // other categories ignored
            }
        }

        // convert total XP per area into level + bar progress
        updateArea(upperBar,  upperLevelLabel,  upperPoints);
        updateArea(lowerBar,  lowerLevelLabel,  lowerPoints);
        updateArea(armsBar,   armsLevelLabel,   armsPoints);
        updateArea(cardioBar, cardioLevelLabel, cardioPoints);

        loadRecentClasses(user.getUserId());
    }

    /** Every 100 XP = 1 level; bar shows XP inside current level. */
    private void updateArea(ProgressBar bar, Label label, int totalPoints) {
        if (bar == null || label == null) return;

        int level     = totalPoints / 100;     // 0,1,2,...
        int xpInLevel = totalPoints % 100;     // 0–99
        double pct    = xpInLevel / 100.0;     // 0.0–1.0

        bar.setProgress(pct);
        label.setText(String.format("Lv %d · %d/100 XP", level, xpInLevel));
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
                            10 // display-only for now
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
