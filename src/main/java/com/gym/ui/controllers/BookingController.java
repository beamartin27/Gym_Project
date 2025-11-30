package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.ClassSchedule;
import com.gym.domain.GymClass;
import com.gym.service.BookingService;
import com.gym.service.ClassService;
import com.gym.ui.utils.SessionManager;
import com.gym.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


public class BookingController {

    private final BookingService bookingService = AppConfig.getBookingService();
    private final ClassService classService = AppConfig.getClassService();

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ISO_LOCAL_DATE;               // 2025-11-29
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");           // 18:00

    @FXML
    private DatePicker datePicker;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<ClassSchedule> scheduleTable;

    @FXML
    private TableColumn<ClassSchedule, String> timeColumn;

    @FXML
    private TableColumn<ClassSchedule, String> classNameColumn;

    @FXML
    private TableColumn<ClassSchedule, String> instructorColumn;

    @FXML
    private TableColumn<ClassSchedule, String> focusColumn;

    @FXML
    private TableColumn<ClassSchedule, String> dateColumn;

    @FXML
    private TableColumn<ClassSchedule, String> spotsColumn;

    @FXML
    private Label selectionLabel;

    private ClassSchedule selectedSchedule;

    /* all schedules for the selected date, before search filter */
    private List<ClassSchedule> allSchedulesForDate = List.of();
    private final Map<Integer, GymClass> classCache = new HashMap<>();

    @FXML
    private void initialize() {
        // limit date picker to "today + next week"
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusWeeks(2).minusDays(1);

        datePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    return;
                }
                boolean disabled = item.isBefore(today) || item.isAfter(maxDate);
                setDisable(disabled);
                if (disabled) {
                    setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #999999;");
                }
            }
        });

        datePicker.setValue(today);
        datePicker.valueProperty().addListener((obs, oldV, newV) -> loadSchedulesForSelectedDate());

        timeColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            String time = TIME_FORMATTER.format(s.getStartTime()) + " - " +
                    TIME_FORMATTER.format(s.getEndTime());
            return new SimpleStringProperty(time);
        });

        classNameColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            GymClass gymClass = classCache.computeIfAbsent(
                    s.getClassId(), id -> classService.getClassById(id)
            );
            String name = (gymClass != null)
                    ? gymClass.getClassName()
                    : "Class #" + s.getClassId();
            return new SimpleStringProperty(name);
        });

        instructorColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            GymClass gymClass = classCache.computeIfAbsent(
                    s.getClassId(), id -> classService.getClassById(id)
            );
            String instructor = (gymClass != null && gymClass.getInstructorName() != null)
                    ? gymClass.getInstructorName()
                    : "—";
            return new SimpleStringProperty(instructor);
        });

        focusColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            GymClass gymClass = classCache.computeIfAbsent(
                    s.getClassId(), id -> classService.getClassById(id)
            );
            String focus = (gymClass != null && gymClass.getClassType() != null)
                    ? focusEmoji(gymClass.getClassType()) + " " + gymClass.getClassType()
                    : "—";
            return new SimpleStringProperty(focus);
        });

        dateColumn.setCellValueFactory(cd ->
                new SimpleStringProperty(DATE_FORMATTER.format(cd.getValue().getScheduledDate())));

        spotsColumn.setCellValueFactory(cd -> {
            ClassSchedule s = cd.getValue();
            int remaining = s.getAvailableSpots();
            return new SimpleStringProperty(String.valueOf(remaining));
        });

        // when you click a row
        scheduleTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            selectedSchedule = newV;
            if (newV != null) {
                selectionLabel.setText("Selected: " + formatSchedule(newV));
            } else {
                selectionLabel.setText("Select a class from the list");
            }
        });

        // search field -> filter table
        searchField.textProperty().addListener((obs, oldV, newV) -> applySearchFilter());

        // initial load
        loadSchedulesForSelectedDate();
    }

    private String focusEmoji(String classType) {
        String type = classType.toUpperCase();
        return switch (type) {
            case "YOGA", "FLEXIBILITY" -> "🧘";
            case "CARDIO", "HIIT", "ENDURANCE" -> "🏃";
            case "STRENGTH", "LEGS" -> "💪";
            case "CORE" -> "🧱";
            default -> "🏋";
        };
    }

    private void loadSchedulesForSelectedDate() {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(14);

        LocalDate selectedDate = datePicker.getValue();
        List<ClassSchedule> schedules;

        if (selectedDate == null) {
            schedules = classService.getAvailableSchedules().stream()
                    .filter(s -> {
                        LocalDate d = s.getScheduledDate();
                        return !d.isBefore(today) && !d.isAfter(maxDate);
                    })
                    .toList();

            selectionLabel.setText("Showing classes for the next 14 days");
        } else {
            if (!selectedDate.isBefore(today) && !selectedDate.isAfter(maxDate)) {
                schedules = classService.getSchedulesByDate(selectedDate);
            } else {
                schedules = List.of();
            }

            if (schedules.isEmpty()) {
                selectionLabel.setText("No classes on " + selectedDate.format(DATE_FORMATTER));
            } else {
                selectionLabel.setText("Select a class from the list");
            }
        }

        allSchedulesForDate = schedules;
        scheduleTable.setItems(FXCollections.observableArrayList(schedules));
        applySearchFilter();
    }


    /** Re-applies text filter over allSchedulesForDate to update the table. */
    private void applySearchFilter() {
        if (allSchedulesForDate == null) {
            return;
        }
        String q = searchField.getText();
        if (q == null) q = "";
        final String query = q.trim().toLowerCase();

        List<ClassSchedule> filtered = allSchedulesForDate;

        if (!query.isEmpty()) {
            filtered = allSchedulesForDate.stream()
                    .filter(s -> {
                        GymClass gc = classService.getClassById(s.getClassId());
                        String className = (gc != null ? gc.getClassName() : "");
                        String instructor = (gc != null ? gc.getInstructorName() : "");
                        String dateStr = s.getScheduledDate().format(DATE_FORMATTER);

                        return className.toLowerCase().contains(query)
                                || instructor.toLowerCase().contains(query)
                                || dateStr.contains(query);
                    })
                    .toList();
        }

        scheduleTable.setItems(FXCollections.observableArrayList(filtered));

        if (filtered.isEmpty()) {
            selectionLabel.setText("No classes available for this date / filter.");
        } else if (selectedSchedule == null || !filtered.contains(selectedSchedule)) {
            selectionLabel.setText("Select a class from the list");
        }
    }

    private String formatSchedule(ClassSchedule s) {
        GymClass gymClass = classCache.computeIfAbsent(
                s.getClassId(), id -> classService.getClassById(id)
        );
        String name = (gymClass != null) ? gymClass.getClassName() : "Class #" + s.getClassId();
        return name + " on " + DATE_FORMATTER.format(s.getScheduledDate()) +
                " at " + TIME_FORMATTER.format(s.getStartTime());
    }

    @FXML
    private void onConfirmClicked() {
        if (selectedSchedule == null) {
            selectionLabel.setText("Please select a class first.");
            return;
        }

        // check if there are still spots (race condition safe-ish)
        if (!selectedSchedule.hasAvailableSpots()) {
            selectionLabel.setText("This class is now full.");
            loadSchedulesForSelectedDate();
            return;
        }

        var currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            selectionLabel.setText("You must be logged in to book.");
            return;
        }

        boolean ok = bookingService.bookClass(
                currentUser.getUserId(),
                selectedSchedule.getScheduleId()
        );

        if (ok) {
            selectionLabel.setText("Class booked! See it in 'My bookings'.");
            // refresh spots
            loadSchedulesForSelectedDate();
        } else {
            selectionLabel.setText("Could not book this class (maybe already booked?).");
        }
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/member-dashboard.fxml", "Member dashboard");
    }

    @FXML
    private void onClearDateClicked() {
        datePicker.setValue(null);      // deja la fecha en null
        loadSchedulesForSelectedDate(); // recarga → muestra próximas 2 semanas
    }
}
