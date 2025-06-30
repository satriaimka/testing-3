package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FocusSessionService {

    /**
     * Record a completed focus session
     */
    public boolean recordFocusSession(int userId, int durationMinutes, Integer taskId, String sessionType) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "INSERT INTO focus_sessions (user_id, task_id, duration_minutes, session_date, session_type) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);

            stmt.setInt(1, userId);
            if (taskId != null) {
                stmt.setInt(2, taskId);
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, durationMinutes);
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setString(5, sessionType); // "FOCUS" or "BREAK"

            int result = stmt.executeUpdate();

            if (result > 0) {
                System.out.println("✅ Focus session recorded: " + durationMinutes + " minutes");
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error recording focus session: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get total focus time for today in minutes
     */
    public int getTodayFocusTime(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT SUM(duration_minutes) FROM focus_sessions WHERE user_id = ? AND session_date = ? AND session_type = 'FOCUS'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting today's focus time: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get focus time for a specific date
     */
    public int getFocusTimeForDate(int userId, LocalDate date) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT SUM(duration_minutes) FROM focus_sessions WHERE user_id = ? AND session_date = ? AND session_type = 'FOCUS'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting focus time for date: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get focus time for the current week
     */
    public int getWeeklyFocusTime(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);

            String query = "SELECT SUM(duration_minutes) FROM focus_sessions WHERE user_id = ? AND session_date >= ? AND session_date <= ? AND session_type = 'FOCUS'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(weekStart));
            stmt.setDate(3, Date.valueOf(today));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting weekly focus time: " + e.getMessage());
        }

        return 0;
    }

    /**
     * Get recent focus sessions for activity feed
     */
    public List<String> getRecentSessions(int userId, int limit) {
        List<String> sessions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT duration_minutes, session_type, created_at FROM focus_sessions WHERE user_id = ? AND session_type = 'FOCUS' ORDER BY created_at DESC LIMIT ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int duration = rs.getInt("duration_minutes");
                Timestamp createdAt = rs.getTimestamp("created_at");

                String timeAgo = getTimeAgo(createdAt.toLocalDateTime());
                sessions.add("🍅 " + duration + "min focus session • " + timeAgo);
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent sessions: " + e.getMessage());
        }

        return sessions;
    }

    /**
     * Get focus session statistics for insights
     */
    public FocusSessionStats getFocusStats(int userId, int days) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            LocalDate startDate = LocalDate.now().minusDays(days - 1);

            String query = """
                SELECT 
                    COUNT(*) as session_count,
                    SUM(duration_minutes) as total_minutes,
                    AVG(duration_minutes) as avg_duration,
                    MAX(duration_minutes) as max_duration
                FROM focus_sessions 
                WHERE user_id = ? AND session_date >= ? AND session_type = 'FOCUS'
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(startDate));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new FocusSessionStats(
                        rs.getInt("session_count"),
                        rs.getInt("total_minutes"),
                        rs.getDouble("avg_duration"),
                        rs.getInt("max_duration")
                );
            }

        } catch (SQLException e) {
            System.err.println("Error getting focus stats: " + e.getMessage());
        }

        return new FocusSessionStats(0, 0, 0.0, 0);
    }

    /**
     * Get daily focus time for the last N days (for charts)
     */
    public List<DailyFocusData> getDailyFocusData(int userId, int days) {
        List<DailyFocusData> data = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT session_date, SUM(duration_minutes) as total_minutes
                FROM focus_sessions 
                WHERE user_id = ? AND session_date >= ? AND session_type = 'FOCUS'
                GROUP BY session_date
                ORDER BY session_date ASC
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(LocalDate.now().minusDays(days - 1)));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                LocalDate date = rs.getDate("session_date").toLocalDate();
                int minutes = rs.getInt("total_minutes");
                data.add(new DailyFocusData(date, minutes));
            }

        } catch (SQLException e) {
            System.err.println("Error getting daily focus data: " + e.getMessage());
        }

        return data;
    }

    /**
     * Calculate focus streak (consecutive days with focus sessions)
     */
    public int getFocusStreak(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            LocalDate currentDate = LocalDate.now();
            int streak = 0;

            for (int i = 0; i < 30; i++) { // Check last 30 days
                LocalDate checkDate = currentDate.minusDays(i);

                String query = "SELECT COUNT(*) FROM focus_sessions WHERE user_id = ? AND session_date = ? AND session_type = 'FOCUS'";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setDate(2, Date.valueOf(checkDate));

                ResultSet rs = stmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    streak++;
                } else if (i > 0) { // Don't break on today if no sessions yet
                    break;
                }
            }

            return streak;

        } catch (SQLException e) {
            System.err.println("Error calculating focus streak: " + e.getMessage());
            return 0;
        }
    }

    private String getTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "Unknown time";

        try {
            LocalDateTime now = LocalDateTime.now();
            long minutes = java.time.Duration.between(dateTime, now).toMinutes();

            if (minutes < 1) return "Just now";
            if (minutes < 60) return minutes + " minutes ago";

            long hours = minutes / 60;
            if (hours < 24) return hours + " hours ago";

            long days = hours / 24;
            return days + " days ago";

        } catch (Exception e) {
            return "Recently";
        }
    }

    // Inner classes for data structures
    public static class FocusSessionStats {
        public final int sessionCount;
        public final int totalMinutes;
        public final double avgDuration;
        public final int maxDuration;

        public FocusSessionStats(int sessionCount, int totalMinutes, double avgDuration, int maxDuration) {
            this.sessionCount = sessionCount;
            this.totalMinutes = totalMinutes;
            this.avgDuration = avgDuration;
            this.maxDuration = maxDuration;
        }
    }

    public static class DailyFocusData {
        public final LocalDate date;
        public final int minutes;

        public DailyFocusData(LocalDate date, int minutes) {
            this.date = date;
            this.minutes = minutes;
        }
    }
}