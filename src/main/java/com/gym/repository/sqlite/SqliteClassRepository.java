package com.gym.repository.sqlite;

import com.gym.domain.GymClass;
import com.gym.domain.ClassSchedule;
import com.gym.repository.ClassRepository;
import com.gym.repository.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class SqliteClassRepository implements ClassRepository {
    private final DatabaseManager dbManager;

    public SqliteClassRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    //Gym Class
    @Override
    public boolean saveClass(GymClass gymClass) {
        String sql = "INSERT INTO classes (class_name, instructor_name, description, capacity, duration_minutes, class_type) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, gymClass.getClassName());
            pstmt.setString(2, gymClass.getInstructorName());
            pstmt.setString(3, gymClass.getDescription());
            pstmt.setInt(4, gymClass.getCapacity());
            pstmt.setInt(5, gymClass.getDurationMinutes());
            pstmt.setString(6, gymClass.getClassType());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        gymClass.setClassId(rs.getInt(1));
                    }
                }
                System.out.println("Class saved: " + gymClass.getClassName());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error saving class: " + e.getMessage());
        }
        return false;
    }

    @Override
    public GymClass findClassById(int classId) {
        String sql = "SELECT * FROM classes WHERE class_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractClassFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding class: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<GymClass> findAllClasses() {
        List<GymClass> classes = new ArrayList<>();
        String sql = "SELECT * FROM classes";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                classes.add(extractClassFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting all classes: " + e.getMessage());
        }
        return classes;
    }

    @Override
    public boolean updateClass(GymClass gymClass) {
        String sql = "UPDATE classes SET class_name = ?, instructor_name = ?, description = ?, capacity = ?, duration_minutes = ?, class_type = ? WHERE class_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, gymClass.getClassName());
            pstmt.setString(2, gymClass.getInstructorName());
            pstmt.setString(3, gymClass.getDescription());
            pstmt.setInt(4, gymClass.getCapacity());
            pstmt.setInt(5, gymClass.getDurationMinutes());
            pstmt.setString(6, gymClass.getClassType());
            pstmt.setInt(7, gymClass.getClassId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Class updated");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating class: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteClass(int classId) {
        String sql = "DELETE FROM classes WHERE class_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, classId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Class deleted");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting class: " + e.getMessage());
        }
        return false;
    }

    //Class Schedule
    @Override
    public boolean saveSchedule(ClassSchedule schedule) {
        String sql = "INSERT INTO class_schedule (class_id, scheduled_date, start_time, end_time, available_spots) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, schedule.getClassId());
            pstmt.setString(2, schedule.getScheduledDate().toString());
            pstmt.setString(3, schedule.getStartTime().toString());
            pstmt.setString(4, schedule.getEndTime().toString());
            pstmt.setInt(5, schedule.getAvailableSpots());

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        schedule.setScheduleId(rs.getInt(1));
                    }
                }
                System.out.println("Schedule saved: ID " + schedule.getScheduleId());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error saving schedule: " + e.getMessage());
        }
        return false;
    }

    @Override
    public ClassSchedule findScheduleById(int scheduleId) {
        String sql = "SELECT * FROM class_schedule WHERE schedule_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, scheduleId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractScheduleFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding schedule: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<ClassSchedule> findSchedulesByClassId(int classId) {
        List<ClassSchedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM class_schedule WHERE class_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, classId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                schedules.add(extractScheduleFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error finding schedules: " + e.getMessage());
        }
        return schedules;
    }

    @Override
    public List<ClassSchedule> findAllSchedules() {
        List<ClassSchedule> schedules = new ArrayList<>();
        String sql = "SELECT * FROM class_schedule";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                schedules.add(extractScheduleFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error getting all schedules: " + e.getMessage());
        }
        return schedules;
    }
    @Override
    public boolean updateSchedule(ClassSchedule schedule) {
        String sql = "UPDATE class_schedule SET scheduled_date = ?, start_time = ?, end_time = ?, available_spots = ? WHERE schedule_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, schedule.getScheduledDate().toString());
            pstmt.setString(2, schedule.getStartTime().toString());
            pstmt.setString(3, schedule.getEndTime().toString());
            pstmt.setInt(4, schedule.getAvailableSpots());
            pstmt.setInt(5, schedule.getScheduleId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Schedule updated");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating schedule: " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteSchedule(int scheduleId) {
        String sql = "DELETE FROM class_schedule WHERE schedule_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, scheduleId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Schedule deleted");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting schedule: " + e.getMessage());
        }
        return false;
    }

    //Helper Methods

    private GymClass extractClassFromResultSet(ResultSet rs) throws SQLException {
        GymClass gymClass = new GymClass(
                rs.getString("class_name"),
                rs.getString("instructor_name"),
                rs.getString("description"),
                rs.getInt("capacity"),
                rs.getInt("duration_minutes"),
                rs.getString("class_type")
        );
        gymClass.setClassId(rs.getInt("class_id"));
        return gymClass;
    }

    private ClassSchedule extractScheduleFromResultSet(ResultSet rs) throws SQLException {
        return new ClassSchedule(
                rs.getInt("schedule_id"),
                rs.getInt("class_id"),
                LocalDate.parse(rs.getString("scheduled_date")),
                LocalTime.parse(rs.getString("start_time")),
                LocalTime.parse(rs.getString("end_time")),
                rs.getInt("available_spots")
        );
    }
}