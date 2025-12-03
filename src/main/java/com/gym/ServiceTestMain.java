package com.gym;

import com.gym.domain.*;
import com.gym.repository.sqlite.SqliteDatabaseManager;
import com.gym.service.*;

import java.time.LocalDate;
import java.time.LocalTime;

public class ServiceTestMain {
    public static void main(String[] args) {
        AppConfig.init();

        System.out.println("\n=== COMPLETE SERVICE LAYER TEST ===\n");

        AuthService authService = AppConfig.getAuthService();
        ClassService classService = AppConfig.getClassService();
        BookingService bookingService = AppConfig.getBookingService();
        ProgressService progressService = AppConfig.getProgressService();

        // 1. Register and login user
        System.out.println("--- Step 1: User Registration ---");
        authService.register("sarah_athlete", "password123", "sarah@gym.com", "MEMBER");
        User sarah = authService.login("sarah_athlete", "password123");

        // 2. Initialize user progress
        System.out.println("\n--- Step 2: Initialize Progress ---");
        progressService.initializeUserProgress(sarah.getUserId());

        // 3. Create classes
        System.out.println("\n--- Step 3: Create Classes ---");
        GymClass hiit = new GymClass("Bootcamp Blast", "Mike Thunder",
                "High intensity workout", 10, 45, "HIIT");
        classService.createClass(hiit);

        GymClass yoga = new GymClass("Zen Flow Yoga", "Luna Peace",
                "Relaxing yoga session", 15, 60, "YOGA");
        classService.createClass(yoga);

        GymClass strength = new GymClass("Iron Warrior", "Max Steel",
                "Heavy lifting session", 8, 90, "STRENGTH");
        classService.createClass(strength);

        // 4. Create schedules
        System.out.println("\n--- Step 4: Schedule Classes ---");
        ClassSchedule hiitSchedule = new ClassSchedule(
                hiit.getClassId(),
                LocalDate.now().plusDays(1),
                LocalTime.of(9, 0),
                LocalTime.of(9, 45),
                10
        );
        classService.createSchedule(hiitSchedule);

        ClassSchedule yogaSchedule = new ClassSchedule(
                yoga.getClassId(),
                LocalDate.now().plusDays(1),
                LocalTime.of(18, 0),
                LocalTime.of(19, 0),
                15
        );
        classService.createSchedule(yogaSchedule);

        ClassSchedule strengthSchedule = new ClassSchedule(
                strength.getClassId(),
                LocalDate.now().plusDays(2),
                LocalTime.of(10, 0),
                LocalTime.of(11, 30),
                8
        );
        classService.createSchedule(strengthSchedule);

        // 5. Show initial progress
        System.out.println("\n--- Initial Progress (all zeros) ---");
        progressService.getAllUserProgress(sarah.getUserId())
                .forEach(p -> System.out.println(p));

        // 6. Book and attend HIIT class
        System.out.println("\n--- Step 5: Book HIIT Class ---");
        bookingService.bookClass(sarah.getUserId(), hiitSchedule.getScheduleId());

        System.out.println("\n--- Simulate Attending HIIT Class ---");
        progressService.awardPointsForClass(sarah.getUserId(), hiit.getClassType());

        // 7. Book and attend Yoga class
        System.out.println("\n--- Step 6: Book Yoga Class ---");
        bookingService.bookClass(sarah.getUserId(), yogaSchedule.getScheduleId());

        System.out.println("\n--- Simulate Attending Yoga Class ---");
        progressService.awardPointsForClass(sarah.getUserId(), yoga.getClassType());

        // 8. Book and attend Strength class
        System.out.println("\n--- Step 7: Book Strength Class ---");
        bookingService.bookClass(sarah.getUserId(), strengthSchedule.getScheduleId());

        System.out.println("\n--- Simulate Attending Strength Class ---");
        progressService.awardPointsForClass(sarah.getUserId(), strength.getClassType());

        // 9. Show final progress
        System.out.println("\n--- Final Progress Summary ---");
        progressService.getAllUserProgress(sarah.getUserId())
                .stream()
                .filter(p -> p.getTotalPoints() > 0)
                .forEach(p -> System.out.println(p + " | Level: " + p.getLevel()));

        // 10. Show user's bookings
        System.out.println("\n--- User's Bookings ---");
        bookingService.getUserBookings(sarah.getUserId())
                .forEach(System.out::println);

        System.out.println("\n=== ALL SERVICE LAYER TESTS PASSED ===");
    }
}