package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.RegularUser;
import com.focusbuddy.models.User;
import com.focusbuddy.utils.PasswordUtils;
import com.focusbuddy.utils.ValidationUtils;

import java.sql.*;
import java.time.LocalDateTime;

/**
 * STEP 2: Buat file UserService.java di package com.focusbuddy.services
 * Service ini menangani operasi database untuk user
 */
public class UserService {
    private final DatabaseManager dbManager;

    public UserService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    /**
     * Ambil data user berdasarkan ID
     */
    public RegularUser getUserById(int userId) throws SQLException {
        String query = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                RegularUser user = new RegularUser();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setFullName(rs.getString("full_name"));

                Timestamp createdTimestamp = rs.getTimestamp("created_at");
                if (createdTimestamp != null) {
                    user.setCreatedAt(createdTimestamp.toLocalDateTime());
                }

                return user;
            }
            return null;
        }
    }

    /**
     * Update profil user (nama dan email)
     */
    public boolean updateUserProfile(User user, String newFullName, String newEmail) throws SQLException {
        // Validasi input
        if (!ValidationUtils.isNotEmpty(newFullName)) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }

        if (!ValidationUtils.isValidEmail(newEmail)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Cek apakah email sudah dipakai user lain
        if (isEmailTaken(newEmail, user.getId())) {
            throw new IllegalArgumentException("Email is already taken by another user");
        }

        String query = "UPDATE users SET full_name = ?, email = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, ValidationUtils.sanitizeInput(newFullName));
            stmt.setString(2, ValidationUtils.sanitizeInput(newEmail));
            stmt.setInt(3, user.getId());

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                // Update object user
                user.setFullName(newFullName);
                user.setEmail(newEmail);
                return true;
            }
            return false;
        }
    }

    /**
     * Ganti password user
     */
    public boolean changePassword(User user, String currentPassword, String newPassword) throws SQLException {
        // Validasi password baru
        if (!ValidationUtils.isValidPassword(newPassword)) {
            throw new IllegalArgumentException("New password must be at least 6 characters long");
        }

        // Ambil data user dari database
        RegularUser dbUser = getUserById(user.getId());
        if (dbUser == null) {
            throw new SQLException("User not found");
        }

        // Verifikasi password lama
        String currentSalt = getSaltForUser(user.getId());
        if (currentSalt == null || !PasswordUtils.verifyPassword(currentPassword, dbUser.getPassword(), currentSalt)) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        // Generate salt baru dan hash password baru
        String newSalt = PasswordUtils.generateSalt();
        String hashedNewPassword = PasswordUtils.hashPassword(newPassword, newSalt);

        String query = "UPDATE users SET password = ?, salt = ? WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, hashedNewPassword);
            stmt.setString(2, newSalt);
            stmt.setInt(3, user.getId());

            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated > 0) {
                user.setPassword(hashedNewPassword);
                return true;
            }
            return false;
        }
    }

    /**
     * Ambil salt untuk user (untuk verifikasi password)
     */
    private String getSaltForUser(int userId) throws SQLException {
        String query = "SELECT salt FROM users WHERE id = ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("salt");
            }
            return null;
        }
    }

    /**
     * Cek apakah email sudah dipakai user lain
     */
    private boolean isEmailTaken(String email, int excludeUserId) throws SQLException {
        String query = "SELECT id FROM users WHERE email = ? AND id != ?";

        try (Connection conn = dbManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, email);
            stmt.setInt(2, excludeUserId);
            ResultSet rs = stmt.executeQuery();

            return rs.next();
        }
    }

    /**
     * Ambil statistik user
     */
    public UserStats getUserStats(int userId) throws SQLException {
        UserStats stats = new UserStats();

        try (Connection conn = dbManager.getConnection()) {
            // Hitung tasks
            String tasksQuery = "SELECT " +
                    "COUNT(*) as total_tasks, " +
                    "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_tasks " +
                    "FROM tasks WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(tasksQuery)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.totalTasks = rs.getInt("total_tasks");
                    stats.completedTasks = rs.getInt("completed_tasks");
                }
            }

            // Hitung focus time
            String focusQuery = "SELECT SUM(duration_minutes) as total_focus_time FROM focus_sessions WHERE user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(focusQuery)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.totalFocusMinutes = rs.getInt("total_focus_time");
                }
            }

            // Hitung goals
            String goalsQuery = "SELECT " +
                    "COUNT(*) as total_goals, " +
                    "SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed_goals " +
                    "FROM goals WHERE user_id = ?";

            try (PreparedStatement stmt = conn.prepareStatement(goalsQuery)) {
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    stats.totalGoals = rs.getInt("total_goals");
                    stats.completedGoals = rs.getInt("completed_goals");
                }
            }
        }

        return stats;
    }

    /**
     * Class untuk menyimpan statistik user
     */
    public static class UserStats {
        public int totalTasks = 0;
        public int completedTasks = 0;
        public int totalFocusMinutes = 0;
        public int totalGoals = 0;
        public int completedGoals = 0;

        public String getFormattedFocusTime() {
            if (totalFocusMinutes == 0) return "0 minutes";

            int hours = totalFocusMinutes / 60;
            int minutes = totalFocusMinutes % 60;

            if (hours == 0) {
                return minutes + " minute" + (minutes != 1 ? "s" : "");
            } else if (minutes == 0) {
                return hours + " hour" + (hours != 1 ? "s" : "");
            } else {
                return hours + "h " + minutes + "m";
            }
        }
    }
}