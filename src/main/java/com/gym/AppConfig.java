package com.gym;

import com.gym.repository.*;
import com.gym.repository.sqlite.*;
import com.gym.service.*;

public class AppConfig {

    private static UserRepository userRepository;
    private static ClassRepository classRepository;
    private static BookingRepository bookingRepository;
    private static ProgressRepository progressRepository;

    private static AuthService authService;
    private static ClassService classService;
    private static BookingService bookingService;
    private static ProgressService progressService;

    public static void init() {
        // REPOSITORIES
        userRepository = new SqliteUserRepository();
        classRepository = new SqliteClassRepository();
        bookingRepository = new SqliteBookingRepository();
        progressRepository = new SqliteProgressRepository();

        // SERVICES
        authService = new AuthServiceImpl(userRepository);
        classService = new ClassServiceImpl(classRepository);
        bookingService = new BookingServiceImpl(bookingRepository, classRepository);
        progressService = new ProgressServiceImpl(progressRepository);
    }

    public static AuthService getAuthService() { return authService; }
    public static ClassService getClassService() { return classService; }
    public static BookingService getBookingService() { return bookingService; }
    public static ProgressService getProgressService() { return progressService; }
    public static UserRepository getUserRepository() { return userRepository; }
    public static BookingRepository getBookingRepository() { return bookingRepository; }
    public static ClassRepository getClassRepository() { return classRepository; }

}
