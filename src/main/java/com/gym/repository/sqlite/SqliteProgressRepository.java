package com.gym.repository.sqlite;

import com.gym.domain.FitnessProgress;
import com.gym.repository.DatabaseManager;
import com.gym.repository.ProgressRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SqliteProgressRepository implements ProgressRepository {

    private final DatabaseManager dbManager;

    public SqliteProgressRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public boolean save(FitnessProgress progress) {
        String sql = "INSERT INTO fitness_progress (user_id, category, total_points, last_updated) VALUES (?, ?, ?, ?)";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, progress.getUserId());
            pstmt.setString(2, progress.getCategory());
            pstmt.setInt(3, progress.getTotalPoints());
            pstmt.setString(4, progress.getLastUpdated().toString());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        progress.setProgressId(rs.getInt(1));
                    }
                }
                System.out.println("Progress saved: " + progress.getCategory());
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
        return false;
    }

    @Override
    public FitnessProgress findById(int progressId) {
        String sql = "SELECT * FROM fitness_progress WHERE progress_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, progressId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractProgressFromResultSet(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error finding progress: " + e.getMessage());
        }
        return null;
    }
    @Override
    public FitnessProgress findByUserIdAndCategory(int userId, String category) {
        String sql = "SELECT * FROM fitness_progress WHERE user_id = ? AND category = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setString(2, category);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractProgressFromResultSet(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding progress: " + e.getMessage());
        }
        return null;
    }
    @Override
    public List<FitnessProgress> findByUserId(int userId) {
        List<FitnessProgress> progressList = new ArrayList<>();
        String sql = "SELECT * FROM fitness_progress WHERE user_id = ? ORDER BY total_points DESC";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                progressList.add(extractProgressFromResultSet(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error finding progress: " + e.getMessage());
        }
        return progressList;
    }
    @Override
    public List<FitnessProgress> findAll() {
        List<FitnessProgress> progressList = new ArrayList<>();
        String sql = "SELECT * FROM fitness_progress ORDER BY total_points DESC";

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                progressList.add(extractProgressFromResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getting all progress: " + e.getMessage());
        }
        return progressList;
    }

    @Override
    public boolean update(FitnessProgress progress) {
        String sql = "UPDATE fitness_progress SET total_points = ?, last_updated = ? WHERE progress_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, progress.getTotalPoints());
            pstmt.setString(2, progress.getLastUpdated().toString());
            pstmt.setInt(3, progress.getProgressId());

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("Progress updated");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating progress: " + e.getMessage());
        }
        return false;
    }
    @Override
    public boolean delete(int progressId) {
        String sql = "DELETE FROM fitness_progress WHERE progress_id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, progressId);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Progress deleted");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting progress: " + e.getMessage());
        }
        return false;
    }

    private FitnessProgress extractProgressFromResultSet(ResultSet rs) throws SQLException {
        return new FitnessProgress(
                rs.getInt("progress_id"),
                rs.getInt("user_id"),
                rs.getString("category"),
                rs.getInt("total_points"),
                LocalDate.parse(rs.getString("last_updated"))
        );
    }
}