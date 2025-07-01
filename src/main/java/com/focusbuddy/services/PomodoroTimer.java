package com.focusbuddy.services;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

import java.util.function.BiConsumer;
import com.focusbuddy.observers.TimerObserver;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Date;
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

        // Save focus session to database if it was a focus session
        if (isFocusSession) {
            saveFocusSessionToDatabase();
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
                System.out.println("⚠️ Focus session too short, not saving to database");
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