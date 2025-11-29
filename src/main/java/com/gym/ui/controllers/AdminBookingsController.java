package com.gym.ui.controllers;

import com.gym.AppConfig;
import com.gym.domain.Booking;
import com.gym.repository.BookingRepository;
import com.gym.utils.SceneManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.LocalDateTime;

public class AdminBookingsController {

    @FXML
    private TableView<Booking> bookingsTable;

    @FXML
    private TableColumn<Booking, Integer> idColumn;

    @FXML
    private TableColumn<Booking, Integer> userIdColumn;

    @FXML
    private TableColumn<Booking, Integer> scheduleIdColumn;

    @FXML
    private TableColumn<Booking, LocalDateTime> bookingDateColumn;

    @FXML
    private TableColumn<Booking, String> statusColumn;

    private final BookingRepository bookingRepository = AppConfig.getBookingRepository();

    @FXML
    public void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("bookingId"));
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));
        scheduleIdColumn.setCellValueFactory(new PropertyValueFactory<>("scheduleId"));
        bookingDateColumn.setCellValueFactory(new PropertyValueFactory<>("bookingDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        bookingsTable.setItems(
                FXCollections.observableArrayList(bookingRepository.findAll())
        );
    }

    @FXML
    private void onBackClicked() {
        SceneManager.switchTo("/views/admin-dashboard.fxml", "Admin dashboard");
    }
}

