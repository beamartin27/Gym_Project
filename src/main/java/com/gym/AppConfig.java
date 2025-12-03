package com.gym;

import com.gym.repository.*;
import com.gym.repository.sqlite.*;
import com.gym.service.*;

public class AppConfig {

    private static DatabaseManager databaseManager;

    private static UserRepository userRepository;
    private static ClassRepository classRepository;
    private static BookingRepository bookingRepository;
    private static ProgressRepository progressRepository;

    private static AuthService authService;
    private static ClassService classService;
    private static BookingService bookingService;
    private static ProgressService progressService;

    public static void init() {
        // 1) Create DB manager and initialize DB
        databaseManager = new SqliteDatabaseManager();
        databaseManager.initializeDatabase();

        // 2) Create repositories with that DB manager
        userRepository = new SqliteUserRepository(databaseManager);
        classRepository = new SqliteClassRepository(databaseManager);
        bookingRepository = new SqliteBookingRepository(databaseManager);
        progressRepository = new SqliteProgressRepository(databaseManager);

        // 3) Create services using the repositories
        authService = new AuthServiceImpl(userRepository);
        classService = new ClassServiceImpl(classRepository);
        bookingService = new BookingServiceImpl(bookingRepository, classRepository);
        progressService = new ProgressServiceImpl(progressRepository);

        DemoDataSeeder.seed();

    }

    public static DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public static AuthService getAuthService() { return authService; }
    public static ClassService getClassService() { return classService; }
    public static BookingService getBookingService() { return bookingService; }
    public static ProgressService getProgressService() { return progressService; }
    public static UserRepository getUserRepository() { return userRepository; }
    public static BookingRepository getBookingRepository() { return bookingRepository; }
    public static ClassRepository getClassRepository() { return classRepository; }

}
