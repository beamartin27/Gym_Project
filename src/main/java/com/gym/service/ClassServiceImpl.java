package com.gym.service;

import com.gym.domain.GymClass;
import com.gym.domain.ClassSchedule;
import com.gym.repository.ClassRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public class ClassServiceImpl implements ClassService {
    private final ClassRepository classRepository;

    public ClassServiceImpl(ClassRepository classRepository) {
        this.classRepository = classRepository;
    }

    @Override
    public boolean createClass(GymClass gymClass) {
        if (gymClass.getClassName() == null || gymClass.getClassName().trim().isEmpty()) {
            System.err.println("Class name cannot be empty");
            return false;
        }
        if (gymClass.getInstructorName() == null || gymClass.getInstructorName().trim().isEmpty()) {
            System.err.println("Instructor name cannot be empty");
            return false;
        }
        if (gymClass.getCapacity() <= 0) {
            System.err.println("Capacity must be positive");
            return false;
        }
        if (gymClass.getDurationMinutes() <= 0) {
            System.err.println("Duration must be positive");
            return false;
        }
        return classRepository.saveClass(gymClass);
    }

    @Override
    public GymClass getClassById(int classId) {
        return classRepository.findClassById(classId);
    }
    @Override
    public List<GymClass> getAllClasses() {
        return classRepository.findAllClasses();
    }
    @Override
    public List<GymClass> searchClasses(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return getAllClasses();
        }
        String search = searchTerm.toLowerCase();
        return classRepository.findAllClasses().stream()
                .filter(c -> c.getClassName().toLowerCase().contains(search) ||
                        c.getInstructorName().toLowerCase().contains(search) ||
                        c.getClassType().toLowerCase().contains(search))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateClass(GymClass gymClass) {
        if (gymClass.getClassId() <= 0) {
            System.err.println("Invalid class ID");
            return false;
        }
        return classRepository.updateClass(gymClass);
    }
    @Override
    public boolean deleteClass(int classId) {
        if (classId <= 0) {
            System.err.println("Invalid class ID");
            return false;
        }
        return classRepository.deleteClass(classId);
    }
    @Override
    public boolean createSchedule(ClassSchedule schedule) {
        if (schedule.getClassId() <= 0) {
            System.err.println("Invalid class ID");
            return false;
        }
        if (schedule.getScheduledDate() == null) {
            System.err.println("Schedule date cannot be null");
            return false;
        }
        if (schedule.getScheduledDate().isBefore(LocalDate.now())) {
            System.err.println("Cannot schedule class in the past");
            return false;
        }
        if (schedule.getStartTime() == null || schedule.getEndTime() == null) {
            System.err.println("Start and end times cannot be null");
            return false;
        }
        if (schedule.getEndTime().isBefore(schedule.getStartTime())) {
            System.err.println("End time must be after start time");
            return false;
        }
        if (schedule.getAvailableSpots() <= 0) {
            System.err.println("Available spots must be positive");
            return false;
        }
        GymClass gymClass = classRepository.findClassById(schedule.getClassId());
        if (gymClass == null) {
            System.err.println("Class not found");
            return false;
        }

        if (schedule.getAvailableSpots() > gymClass.getCapacity()) {
            System.err.println("Available spots cannot exceed class capacity (" + gymClass.getCapacity() + ")");
            return false;
        }

        return classRepository.saveSchedule(schedule);
    }

    @Override
    public ClassSchedule getScheduleById(int scheduleId) {
        return classRepository.findScheduleById(scheduleId);
    }
    @Override
    public List<ClassSchedule> getSchedulesByClassId(int classId) {
        return classRepository.findSchedulesByClassId(classId);
    }
    @Override
    public List<ClassSchedule> getAvailableSchedules() {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(14);

        return classRepository.findAllSchedules().stream()
                .filter(s -> s.getAvailableSpots() > 0)
                .filter(s -> {
                    LocalDate d = s.getScheduledDate();
                    return !d.isBefore(today) && !d.isAfter(maxDate);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ClassSchedule> getSchedulesByDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate maxDate = today.plusDays(14);

        // reject invalid dates early
        if (date.isBefore(today) || date.isAfter(maxDate)) {
            return List.of();
        }

        return classRepository.findAllSchedules().stream()
                .filter(s -> s.getScheduledDate().equals(date))
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateSchedule(ClassSchedule schedule) {
        if (schedule.getScheduleId() <= 0) {
            System.err.println("Invalid schedule ID");
            return false;
        }
        return classRepository.updateSchedule(schedule);
    }
    @Override
    public boolean deleteSchedule(int scheduleId) {
        if (scheduleId <= 0) {
            System.err.println("Invalid schedule ID");
            return false;
        }
        return classRepository.deleteSchedule(scheduleId);
    }
}