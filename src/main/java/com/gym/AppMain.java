package com.gym;

import com.gym.domain.User;
import com.gym.domain.GymClass;
import com.gym.domain.ClassSchedule;
import com.gym.repository.UserRepository;
import com.gym.service.ClassService;
import com.gym.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalTime;

public class AppMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1) DB + services
        AppConfig.init();
        seedDefaultUsers();
        seedDemoClassesAndSchedules();

        // 2) Save principal stage
        SceneManager.setPrimaryStage(stage);

        // 3) Show login
        SceneManager.switchTo("/views/login.fxml", "Gym Class Booking - Login");
    }

    private void seedDefaultUsers() {
        UserRepository userRepo = AppConfig.getUserRepository();

        seedUserIfMissing(userRepo, "admin",  "admin123",  "ADMIN");
        seedUserIfMissing(userRepo, "member", "member123", "MEMBER");
        seedUserIfMissing(userRepo, "trainer","trainer123","TRAINER");
    }

    private void seedUserIfMissing(UserRepository repo,
                                   String username,
                                   String rawPassword,
                                   String role) {
        User existing = repo.findByUsername(username);
        if (existing != null) return;

        User u = new User(username, rawPassword, username + "@gym.com", role);
        boolean ok = repo.save(u);
        if (ok) System.out.println("Seeded " + role + " user: " + username + " / " + rawPassword);
    }

    private void seedDemoClassesAndSchedules() {
        ClassService classService = AppConfig.getClassService();

        // If there are already classes in the DB, don't reseed
        if (!classService.getAllClasses().isEmpty()) {
            System.out.println("Classes already exist, skipping demo seed.");
            return;
        }

        System.out.println("Seeding demo classes and schedules...");

        // --- Create some demo classes ---
        GymClass hiit = new GymClass(
                "Bootcamp Blast",
                "Mike Thunder",
                "High intensity HIIT workout",
                10,
                45,
                "HIIT"
        );
        classService.createClass(hiit);

        GymClass yoga = new GymClass(
                "Zen Flow Yoga",
                "Luna Peace",
                "Relaxing yoga session",
                15,
                60,
                "YOGA"
        );
        classService.createClass(yoga);

        GymClass strength = new GymClass(
                "Iron Warrior",
                "Max Steel",
                "Full body strength training",
                8,
                60,
                "STRENGTH"
        );
        classService.createClass(strength);

        LocalDate today = LocalDate.now();

        // --- Schedule them (for TODAY so they appear immediately in Book a class) ---
        ClassSchedule hiitToday = new ClassSchedule(
                hiit.getClassId(),
                today,
                LocalTime.of(9, 0),
                LocalTime.of(9, 45),
                hiit.getCapacity()
        );
        classService.createSchedule(hiitToday);

        ClassSchedule yogaToday = new ClassSchedule(
                yoga.getClassId(),
                today,
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                yoga.getCapacity()
        );
        classService.createSchedule(yogaToday);

        ClassSchedule strengthToday = new ClassSchedule(
                strength.getClassId(),
                today,
                LocalTime.of(19, 30),
                LocalTime.of(20, 30),
                strength.getCapacity()
        );
        classService.createSchedule(strengthToday);

        System.out.println("Demo classes + schedules seeded.");
    }


    public static void main(String[] args) {
        launch();
    }
}
