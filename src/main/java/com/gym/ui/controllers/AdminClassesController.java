package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.GymClass;
import com.gym.domain.User;
import com.gym.repository.UserRepository;
import com.gym.service.ClassService;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Callback;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdminClassesController {

    @FXML
    private TableView<GymClass> classesTable;

    @FXML
    private TableColumn<GymClass, Integer> idColumn;

    @FXML
    private TableColumn<GymClass, String> nameColumn;

    @FXML
    private TableColumn<GymClass, String> instructorColumn;

    @FXML
    private TableColumn<GymClass, String> typeColumn;

    @FXML
    private TableColumn<GymClass, Integer> capacityColumn;

    @FXML
    private TableColumn<GymClass, Integer> durationColumn;

    @FXML
    private TableColumn<GymClass, String> descriptionColumn; // NEW

    @FXML
    private TextField searchField;

    private final ClassService classService = AppConfig.getClassService();
    private final UserRepository userRepository = AppConfig.getUserRepository();

    // All trainers (users with role = TRAINER)
    private ObservableList<User> trainerList;

    @FXML
    public void initialize() {
        // Table columns ← GymClass properties
        idColumn.setCellValueFactory(new PropertyValueFactory<>("classId"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("className"));
        instructorColumn.setCellValueFactory(new PropertyValueFactory<>("instructorName"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("classType"));
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        durationColumn.setCellValueFactory(new PropertyValueFactory<>("durationMinutes"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        loadTrainers();
        loadClasses();
    }

    private void loadTrainers() {
        List<User> all = userRepository.findAll();
        trainerList = FXCollections.observableArrayList(
                all.stream().filter(User::isTrainer).collect(Collectors.toList())
        );
    }

    private void loadClasses() {
        classesTable.setItems(
                FXCollections.observableArrayList(classService.getAllClasses())
        );
    }

    // ─── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }

    // ─── Search ────────────────────────────────────────────────────────────────

    @FXML
    private void onSearchClicked() {
        String term = searchField.getText();
        // assumes ClassService has searchClasses(String)
        classesTable.setItems(
                FXCollections.observableArrayList(classService.searchClasses(term))
        );
    }

    @FXML
    private void onClearSearchClicked() {
        searchField.clear();
        loadClasses();
    }

    // ─── Add / Edit / Delete actions ──────────────────────────────────────────

    @FXML
    private void onAddClass() {
        Optional<GymClass> result = showClassDialog(null);
        result.ifPresent(gymClass -> {
            boolean ok = classService.createClass(gymClass);
            if (ok) {
                loadClasses();
            } else {
                showError("Could not save class in the database.");
            }
        });
    }

    @FXML
    private void onEditClass() {
        GymClass selected = classesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select a class to edit.");
            return;
        }

        Optional<GymClass> result = showClassDialog(selected);
        result.ifPresent(updated -> {
            boolean ok = classService.updateClass(updated);
            if (ok) {
                loadClasses();
            } else {
                showError("Could not update class in the database.");
            }
        });
    }

    @FXML
    private void onDeleteClass() {
        GymClass selected = classesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showInfo("Please select a class to delete.");
            return;
        }

        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Delete class");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete '" +
                selected.getClassName() + "'?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        boolean ok = classService.deleteClass(selected.getClassId());
        if (ok) {
            loadClasses();
        } else {
            showError("Could not delete class. There may be schedules or bookings.");
        }
    }

    // ─── Dialog for add / edit ────────────────────────────────────────────────
    /**
     * If existing == null → Add mode.
     * If existing != null → Edit mode.
     * IMPORTANT: we IGNORE trainerId and only store instructorName = trainer.username
     */
    private Optional<GymClass> showClassDialog(GymClass existing) {
        Dialog<GymClass> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Add class" : "Edit class");
        dialog.setHeaderText(null);

        ButtonType saveButtonType =
                new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
                saveButtonType, ButtonType.CANCEL
        );

        TextField nameField = new TextField();
        TextArea descriptionArea = new TextArea();
        descriptionArea.setPrefRowCount(3);

        Spinner<Integer> capacitySpinner = new Spinner<>(1, 500, 20);
        Spinner<Integer> durationSpinner = new Spinner<>(10, 240, 60);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("YOGA", "CARDIO", "HIIT", "STRENGTH");

        ComboBox<User> instructorCombo = new ComboBox<>();
        instructorCombo.setItems(trainerList);

        // Show username inside the combo
        Callback<ListView<User>, ListCell<User>> cellFactory = lv -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername());
            }
        };
        instructorCombo.setCellFactory(cellFactory);
        instructorCombo.setButtonCell(cellFactory.call(null));

        // Prefill if editing
        if (existing != null) {
            nameField.setText(existing.getClassName());
            descriptionArea.setText(existing.getDescription());
            capacitySpinner.getValueFactory().setValue(existing.getCapacity());
            durationSpinner.getValueFactory().setValue(existing.getDurationMinutes());
            typeCombo.setValue(existing.getClassType());

            // Match trainer by username
            for (User t : trainerList) {
                if (t.getUsername().equalsIgnoreCase(existing.getInstructorName())) {
                    instructorCombo.setValue(t);
                    break;
                }
            }
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.addRow(0, new Label("Name:"), nameField);
        grid.addRow(1, new Label("Instructor:"), instructorCombo);
        grid.addRow(2, new Label("Type:"), typeCombo);
        grid.addRow(3, new Label("Capacity:"), capacitySpinner);
        grid.addRow(4, new Label("Duration (min):"), durationSpinner);
        grid.addRow(5, new Label("Description:"), descriptionArea);

        dialog.getDialogPane().setContent(grid);

        // Disable save button if required fields are empty
        dialog.getDialogPane().lookupButton(saveButtonType).disableProperty().bind(
                nameField.textProperty().isEmpty()
                        .or(instructorCombo.valueProperty().isNull())
                        .or(typeCombo.valueProperty().isNull())
        );

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                String name = nameField.getText().trim();
                User trainer = instructorCombo.getValue();
                String type = typeCombo.getValue();
                int capacity = capacitySpinner.getValue();
                int duration = durationSpinner.getValue();
                String description = descriptionArea.getText().trim();

                // We IGNORE trainerId and only store instructorName (username)
                GymClass gc = new GymClass(
                        name,
                        trainer.getUsername(),
                        description,
                        capacity,
                        duration,
                        type
                );

                if (existing != null) {
                    gc.setClassId(existing.getClassId());
                }

                return gc;
            }
            return null;
        });

        return dialog.showAndWait();
    }

    // ─── Helper alerts ────────────────────────────────────────────────────────

    private void showInfo(String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
