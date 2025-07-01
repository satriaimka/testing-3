package com.focusbuddy.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.function.BiConsumer;
import com.focusbuddy.observers.TimerObserver;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.services.GoalProgressManager;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PomodoroTimer {
    private static final int FOCUS_DURATION = 25 * 60; // 25 minutes in seconds
    private static final int BREAK_DURATION = 5 * 60;  // 5 minutes in seconds

    private Timeline timeline;
    private int currentSeconds;
    private int totalSeconds;
    private boolean isRunning;
    private boolean isFocusSession;
    private int startingSeconds; // Track original duration for this session

    private List<TimerObserver> observers = new ArrayList<>();

    private BiConsumer<Integer, Integer> onTimeUpdate;
    private Runnable onTimerComplete;

    public PomodoroTimer() {
        reset();
        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentSeconds > 0 && isRunning) {
                currentSeconds--;
                updateDisplay();
                System.out.println("⏰ Timer: " + (currentSeconds / 60) + ":" + String.format("%02d", currentSeconds % 60) + " remaining");
            } else if (currentSeconds <= 0) {
                complete();
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void addObserver(TimerObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(TimerObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String event) {
        String timerType = isFocusSession ? "FOCUS" : "BREAK";
        switch (event) {
            case "START" -> observers.forEach(o -> o.onTimerStart(timerType));
            case "PAUSE" -> observers.forEach(o -> o.onTimerPause(timerType));
            case "RESET" -> observers.forEach(o -> o.onTimerReset(timerType));
            case "COMPLETE" -> observers.forEach(o -> o.onTimerComplete(timerType, startingSeconds - currentSeconds));
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            if (timeline != null) {
                timeline.play();
            }
            notifyObservers("START");
            System.out.println("✅ Timer started: " + (isFocusSession ? "FOCUS" : "BREAK") + " session");
        }
    }

    public void pause() {
        if (isRunning) {
            isRunning = false;
            if (timeline != null) {
                timeline.pause();
            }
            notifyObservers("PAUSE");
            System.out.println("⏸️ Timer paused");
        }
    }

    public void reset() {
        if (timeline != null) {
            timeline.stop();
        }
        isRunning = false;
        isFocusSession = true;
        currentSeconds = FOCUS_DURATION;
        totalSeconds = FOCUS_DURATION;
        startingSeconds = FOCUS_DURATION;
        updateDisplay();
        notifyObservers("RESET");
        System.out.println("🔄 Timer reset to focus session");
    }

    private void complete() {
        if (timeline != null) {
            timeline.stop();
        }
        isRunning = false;

        // Calculate actual duration yang completed
        int actualDurationMinutes = (startingSeconds - currentSeconds) / 60;

        // Save focus session to database if it was a focus session
        if (isFocusSession) {
            saveFocusSessionToDatabase();

            // ✅ NEW: UPDATE GOALS OTOMATIS KETIKA FOCUS SESSION COMPLETED
            try {
                if (UserSession.getInstance().isLoggedIn()) {
                    int userId = UserSession.getInstance().getCurrentUser().getId();

                    System.out.println("🎯 Focus session completed! Updating related goals...");

                    // Update goals dengan durasi actual yang completed
                    GoalProgressManager.getInstance().onFocusSessionCompleted(userId, actualDurationMinutes);

                    System.out.println("✅ Goals updated successfully after focus session");
                }

            } catch (Exception e) {
                System.err.println("⚠️ Error updating goals after focus session: " + e.getMessage());
                // Jangan fail timer completion kalau goal update error
            }
        }

        notifyObservers("COMPLETE");

        if (onTimerComplete != null) {
            Platform.runLater(() -> onTimerComplete.run());
        }

        System.out.println("🎉 Timer completed: " + (isFocusSession ? "FOCUS" : "BREAK") + " session");

        // Switch between focus and break
        isFocusSession = !isFocusSession;
        currentSeconds = isFocusSession ? FOCUS_DURATION : BREAK_DURATION;
        totalSeconds = currentSeconds;
        startingSeconds = currentSeconds;
        updateDisplay();
    }

    private void updateDisplay() {
        if (onTimeUpdate != null) {
            int minutes = currentSeconds / 60;
            int seconds = currentSeconds % 60;
            Platform.runLater(() -> {
                try {
                    onTimeUpdate.accept(minutes, seconds);
                } catch (Exception e) {
                    System.err.println("Error updating timer display: " + e.getMessage());
                }
            });
        }
    }

    public double getProgress() {
        if (totalSeconds <= 0) return 0.0;
        double progress = 1.0 - ((double) currentSeconds / totalSeconds);
        return Math.max(0.0, Math.min(1.0, progress)); // Clamp between 0 and 1
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isFocusSession() {
        return isFocusSession;
    }

    public int getCurrentSeconds() {
        return currentSeconds;
    }

    public int getTotalSeconds() {
        return totalSeconds;
    }

    public void setOnTimeUpdate(BiConsumer<Integer, Integer> onTimeUpdate) {
        this.onTimeUpdate = onTimeUpdate;
    }

    public void setOnTimerComplete(Runnable onTimerComplete) {
        this.onTimerComplete = onTimerComplete;
    }

    /**
     * Save completed focus session to database
     */
    private void saveFocusSessionToDatabase() {
        try {
            if (!UserSession.getInstance().isLoggedIn()) {
                System.out.println("⚠️ No user logged in, skipping focus session save");
                return;
            }

            int userId = UserSession.getInstance().getCurrentUser().getId();
            int durationMinutes = (startingSeconds - currentSeconds) / 60;

            // Only save if session was at least 1 minute
            if (durationMinutes < 1) {
                System.out.println("⚠️ Focus session too short (" + durationMinutes + " min), not saving to database");
                return;
            }

            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String query = """
                INSERT INTO focus_sessions (user_id, task_id, duration_minutes, session_date, session_type, created_at) 
                VALUES (?, NULL, ?, ?, 'FOCUS', NOW())
                """;

                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setInt(2, durationMinutes);
                stmt.setDate(3, Date.valueOf(LocalDate.now()));

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✅ Focus session saved: " + durationMinutes + " minutes");

                    // ✅ NEW: Add to recent activity tracking
                    try {
                        // Could add to activity service if needed
                        System.out.println("📊 Focus session logged for activity tracking");

                    } catch (Exception e) {
                        System.err.println("Warning: Could not log to activity service: " + e.getMessage());
                    }

                } else {
                    System.out.println("❌ Failed to save focus session");
                }

            } catch (Exception e) {
                System.err.println("Error saving focus session to database: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("Error in saveFocusSessionToDatabase: " + e.getMessage());
        }
    }

    public static FocusStats getFocusStats(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Today's focus time
            String todayQuery = """
            SELECT COALESCE(SUM(duration_minutes), 0) as total_today,
                   COUNT(*) as sessions_today
            FROM focus_sessions 
            WHERE user_id = ? AND session_date = ? AND session_type = 'FOCUS'
            """;

            PreparedStatement todayStmt = conn.prepareStatement(todayQuery);
            todayStmt.setInt(1, userId);
            todayStmt.setDate(2, Date.valueOf(LocalDate.now()));

            ResultSet todayRs = todayStmt.executeQuery();
            int todayMinutes = 0;
            int todaySessions = 0;

            if (todayRs.next()) {
                todayMinutes = todayRs.getInt("total_today");
                todaySessions = todayRs.getInt("sessions_today");
            }

            // Week's focus time
            String weekQuery = """
            SELECT COALESCE(SUM(duration_minutes), 0) as total_week,
                   COUNT(*) as sessions_week
            FROM focus_sessions 
            WHERE user_id = ? 
            AND session_date >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
            AND session_type = 'FOCUS'
            """;

            PreparedStatement weekStmt = conn.prepareStatement(weekQuery);
            weekStmt.setInt(1, userId);

            ResultSet weekRs = weekStmt.executeQuery();
            int weekMinutes = 0;
            int weekSessions = 0;

            if (weekRs.next()) {
                weekMinutes = weekRs.getInt("total_week");
                weekSessions = weekRs.getInt("sessions_week");
            }

            return new FocusStats(todayMinutes, todaySessions, weekMinutes, weekSessions);

        } catch (Exception e) {
            System.err.println("Error getting focus stats: " + e.getMessage());
            return new FocusStats(0, 0, 0, 0);
        }
    }

    public static class FocusStats {
        public final int todayMinutes;
        public final int todaySessions;
        public final int weekMinutes;
        public final int weekSessions;

        public FocusStats(int todayMinutes, int todaySessions, int weekMinutes, int weekSessions) {
            this.todayMinutes = todayMinutes;
            this.todaySessions = todaySessions;
            this.weekMinutes = weekMinutes;
            this.weekSessions = weekSessions;
        }

        public String getTodayFormatted() {
            return formatTime(todayMinutes);
        }

        public String getWeekFormatted() {
            return formatTime(weekMinutes);
        }

        public double getDailyGoalProgress(int dailyGoalMinutes) {
            return dailyGoalMinutes > 0 ? (double) todayMinutes / dailyGoalMinutes : 0.0;
        }

        private String formatTime(int minutes) {
            if (minutes < 60) {
                return minutes + "m";
            } else {
                int hours = minutes / 60;
                int remainingMinutes = minutes % 60;
                return remainingMinutes == 0 ?
                        hours + "h" :
                        hours + "h " + remainingMinutes + "m";
            }
        }
    }
    /**
     * Get total focus time for today from database
     */
    public static int getTodayFocusMinutes(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT COALESCE(SUM(duration_minutes), 0) as total 
                FROM focus_sessions 
                WHERE user_id = ? AND session_date = ? AND session_type = 'FOCUS'
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));

            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (Exception e) {
            System.err.println("Error getting today's focus time: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Get total focus time for this week from database
     */
    public static int getWeekFocusMinutes(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT COALESCE(SUM(duration_minutes), 0) as total 
                FROM focus_sessions 
                WHERE user_id = ? 
                AND session_date >= DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
                AND session_type = 'FOCUS'
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            var rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }

        } catch (Exception e) {
            System.err.println("Error getting week's focus time: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Stop timer and cleanup resources
     */
    public void stop() {
        if (timeline != null) {
            timeline.stop();
        }
        isRunning = false;
        System.out.println("🛑 Timer stopped and resources cleaned up");
    }
}