package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.Booking;
import com.gym.domain.User;
import com.gym.repository.UserRepository;
import com.gym.service.BookingService;
import com.gym.service.ProgressService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;


public class TrainerAttendanceController {

    @FXML
    private StackPane rootPane;

    @FXML
    private ImageView bgImage;

    @FXML
    private Rectangle overlay;

    @FXML
    private Label headerLabel;

    @FXML
    private Button saveButton;

    @FXML
    private TableView<AttendanceRow> attendanceTable;
    @FXML
    private TableColumn<AttendanceRow, String> memberColumn;
    @FXML
    private TableColumn<AttendanceRow, String> statusColumn;
    @FXML
    private TableColumn<AttendanceRow, Boolean> attendedColumn;

    @FXML
    private Label messageLabel;

    private BookingService bookingService;
    private UserRepository userRepository;

    private int scheduleId;
    private String className;
    private String classType;
    private boolean awardMode = true;

    @FXML
    private void initialize() {
        bookingService = AppConfig.getBookingService();
        userRepository = AppConfig.getUserRepository();

        memberColumn.setCellValueFactory(c -> c.getValue().memberNameProperty());
        statusColumn.setCellValueFactory(c -> c.getValue().statusProperty());

        // Table + column must be editable for the checkbox to toggle
        attendanceTable.setEditable(true);
        attendedColumn.setEditable(true);

        // Bind to the BooleanProperty
        attendedColumn.setCellValueFactory(c -> c.getValue().attendedProperty());

        // Use helper that wires the checkbox to that property
        attendedColumn.setCellFactory(
                CheckBoxTableCell.forTableColumn(attendedColumn)
        );

        // === fondo responsive ===
        bgImage.fitWidthProperty().bind(rootPane.widthProperty());
        bgImage.fitHeightProperty().bind(rootPane.heightProperty());
        overlay.widthProperty().bind(rootPane.widthProperty());
        overlay.heightProperty().bind(rootPane.heightProperty());

        memberColumn.setCellValueFactory(c -> c.getValue().memberNameProperty());
        statusColumn.setCellValueFactory(c -> c.getValue().statusProperty());

        attendanceTable.setEditable(true);
        attendedColumn.setEditable(true);
        attendedColumn.setCellValueFactory(c -> c.getValue().attendedProperty());
        attendedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(attendedColumn));
    }

    /** Called from TrainerDashboardController after FXML is loaded */
    public void setContext(int scheduleId, String className, String classType) {
        setContext(scheduleId, className, classType, true);
    }

    public void setContext(int scheduleId, String className, String classType, boolean awardMode) {
        this.scheduleId = scheduleId;
        this.className = className;
        this.classType = classType;
        this.awardMode = awardMode;

        if (awardMode) {
            headerLabel.setText("Attendance – " + className);
            attendanceTable.setEditable(true);
            attendedColumn.setVisible(true);

            saveButton.setDisable(false);
            saveButton.setVisible(true);
            saveButton.setManaged(true);   // <<--- AÑADIDO

            messageLabel.setText("");
        } else {
            headerLabel.setText("Bookings – " + className);
            attendanceTable.setEditable(false);
            attendedColumn.setVisible(false);

            saveButton.setDisable(true);
            saveButton.setVisible(false);
            saveButton.setManaged(false);  // <<--- AÑADIDO

            messageLabel.setText("View only – attendance cannot be edited.");
        }

        loadBookings();
    }

    private void loadBookings() {
        List<Booking> bookings = bookingService.getScheduleBookings(scheduleId)
                .stream()
                .filter(b -> !b.isCancelled())  // show CONFIRMED + ATTENDED
                .toList();
        List<AttendanceRow> rows = new ArrayList<>();

        for (Booking b : bookings) {
            User u = userRepository.findById(b.getUserId());
            String name = (u != null) ? u.getUsername() : ("User #" + b.getUserId());
            rows.add(new AttendanceRow(
                    b.getBookingId(),
                    b.getUserId(),
                    name,
                    b.getStatus(),
                    b.isAttended()   // checkbox reflects stored attendance
            ));
        }

        attendanceTable.setItems(FXCollections.observableArrayList(rows));
        if (rows.isEmpty()) {
            messageLabel.setText("No bookings for this class.");
        } else {
            messageLabel.setText("");
        }
    }

    @FXML
    private void onSaveClicked() {
        if (!awardMode) {
            return; // in view mode do nothing
        }

        int awarded = 0;

        for (AttendanceRow row : attendanceTable.getItems()) {
            if (row.isAttended()) {
                // BookingService will:
                //  - mark booking as ATTENDED
                //  - award XP only if it wasn't attended before
                boolean ok = bookingService.markAttended(row.getBookingId());
                if (ok) {
                    awarded++;
                }
            }
        }

        if (awarded == 0) {
            messageLabel.setText("No attendees selected or all already marked as attended.");
        } else {
            messageLabel.setText("Saved attendance and awarded points to " + awarded + " member(s).");
        }

        // Refresh table so statuses/checkboxes match DB
        loadBookings();
    }


    @FXML
    private void onCloseClicked() {
        Stage stage = (Stage) attendanceTable.getScene().getWindow();
        stage.close();
    }

    /** Small view-model for each row in the table */
    public static class AttendanceRow {
        private final int bookingId;
        private final int userId;
        private final StringProperty memberName;
        private final StringProperty status;
        private final BooleanProperty attended;

        public AttendanceRow(int bookingId, int userId,
                             String memberName, String status, boolean attended) {
            this.bookingId = bookingId;
            this.userId = userId;
            this.memberName = new SimpleStringProperty(memberName);
            this.status = new SimpleStringProperty(status);
            this.attended = new SimpleBooleanProperty(attended);
        }

        public int getBookingId() { return bookingId; }
        public int getUserId() { return userId; }

        public StringProperty memberNameProperty() { return memberName; }
        public StringProperty statusProperty() { return status; }
        public BooleanProperty attendedProperty() { return attended; }
        public boolean isAttended() { return attended.get(); }
    }
}
