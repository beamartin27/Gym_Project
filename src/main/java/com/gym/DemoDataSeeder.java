package com.gym;

import com.gym.domain.GymClass;
import com.gym.domain.ClassSchedule;
import com.gym.service.ClassService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class DemoDataSeeder {

    public static void seed() {
        ClassService classService = AppConfig.getClassService();

        if (!classService.getAllClasses().isEmpty()) {
            System.out.println("Demo seed skipped (classes already exist)");
            return;
        }

        System.out.println("Seeding demo classes and schedules...");

        GymClass yoga = new GymClass(
                "Morning Yoga", "Ana Lopez",
                "Relaxing yoga session", 20,
                60, "YOGA"
        );

        GymClass hiit = new GymClass(
                "HIIT Blast", "Carlos Ruiz",
                "High-intensity interval training", 15,
                45, "HIIT"
        );

        GymClass strength = new GymClass(
                "Strength Training", "Laura Gomez",
                "Full-body strength workout", 18,
                60, "STRENGTH"
        );

        GymClass cardio = new GymClass(
                "Cardio Burn", "Mateo Diaz",
                "Fat-burning cardio session", 25,
                40, "CARDIO"
        );

        List<GymClass> demoClasses = List.of(yoga, hiit, strength, cardio);

        for (GymClass gc : demoClasses) {
            classService.createClass(gc);
        }

        LocalDate today = LocalDate.now();

        createSchedule(classService, yoga, today.plusDays(1), "09:00");
        createSchedule(classService, yoga, today.plusDays(2), "09:00");

        createSchedule(classService, hiit, today.plusDays(1), "18:00");
        createSchedule(classService, hiit, today.plusDays(3), "18:00");

        createSchedule(classService, strength, today.plusDays(2), "12:00");
        createSchedule(classService, strength, today.plusDays(4), "12:00");

        createSchedule(classService, cardio, today.plusDays(1), "17:00");
        createSchedule(classService, cardio, today.plusDays(3), "17:00");

        System.out.println("Demo seed completed!");
    }

    private static void createSchedule(ClassService classService, GymClass gc, LocalDate date, String startTimeStr) {
        LocalTime start = LocalTime.parse(startTimeStr);
        LocalTime end = start.plusMinutes(gc.getDurationMinutes());

        ClassSchedule schedule = new ClassSchedule(
                gc.getClassId(),
                date,
                start,
                end,
                gc.getCapacity()
        );

        classService.createSchedule(schedule);
    }
}
