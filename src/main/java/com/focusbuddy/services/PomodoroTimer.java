package com.focusbuddy.services;

import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.NotificationManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

import java.util.function.BiConsumer;
import com.focusbuddy.observers.TimerObserver;
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
    private int sessionStartSeconds; // Track how much time was actually used
    private Integer associatedTaskId; // Optional task association

    private List<TimerObserver> observers = new ArrayList<>();
    private FocusSessionService focusSessionService;

    private BiConsumer<Integer, Integer> onTimeUpdate;
    private Runnable onTimerComplete;

    public PomodoroTimer() {
        this.focusSessionService = new FocusSessionService();
        reset();
        setupTimeline();
    }

    private void setupTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (currentSeconds > 0) {
                currentSeconds--;
                updateDisplay();
            } else {
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
            case "COMPLETE" -> observers.forEach(o -> o.onTimerComplete(timerType, totalSeconds - currentSeconds));
        }
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            sessionStartSeconds = currentSeconds; // Record start time for session calculation
            timeline.play();
            notifyObservers("START");

            System.out.println("🍅 " + (isFocusSession ? "Focus" : "Break") + " session started");
        }
    }

    public void pause() {
        if (isRunning) {
            isRunning = false;
            timeline.pause();
            notifyObservers("PAUSE");

            // Record partial session if it was a meaningful amount of time (>5 minutes)
            int elapsedSeconds = sessionStartSeconds - currentSeconds;
            if (elapsedSeconds >= 300 && isFocusSession) { // 5 minutes minimum
                recordSession(elapsedSeconds / 60); // Convert to minutes
            }

            System.out.println("⏸️ Timer paused");
        }
    }

    public void reset() {
        if (timeline != null) {
            timeline.stop();
        }

        // Record partial session if timer was running and meaningful time elapsed
        if (isRunning && isFocusSession) {
            int elapsedSeconds = sessionStartSeconds - currentSeconds;
            if (elapsedSeconds >= 300) { // 5 minutes minimum
                recordSession(elapsedSeconds / 60);
            }
        }

        isRunning = false;
        isFocusSession = true;
        currentSeconds = FOCUS_DURATION;
        totalSeconds = FOCUS_DURATION;
        sessionStartSeconds = currentSeconds;
        updateDisplay();
        notifyObservers("RESET");

        System.out.println("🔄 Timer reset");
    }

    private void complete() {
        timeline.stop();
        isRunning = false;

        // Calculate actual session duration
        int sessionDuration = (sessionStartSeconds - currentSeconds) / 60; // Convert to minutes

        // Record completed session to database
        if (sessionDuration > 0) {
            recordSession(sessionDuration);
        }

        notifyObservers("COMPLETE");

        if (onTimerComplete != null) {
            onTimerComplete.run();
        }

        // Show completion notification
        String sessionType = isFocusSession ? "Focus" : "Break";
        String message = sessionType + " session completed! ";
        message += isFocusSession ? "Time for a well-deserved break. 🎉" : "Ready for another focus session? 💪";

        NotificationManager.getInstance().showNotification(
                sessionType + " Session Complete!",
                message,
                NotificationManager.NotificationType.SUCCESS
        );

        // Switch between focus and break
        switchSession();

        System.out.println("✅ " + sessionType + " session completed: " + sessionDuration + " minutes");
    }

    private void switchSession() {
        isFocusSession = !isFocusSession;
        currentSeconds = isFocusSession ? FOCUS_DURATION : BREAK_DURATION;
        totalSeconds = currentSeconds;
        sessionStartSeconds = currentSeconds;
        updateDisplay();
    }

    private void recordSession(int durationMinutes) {
        try {
            if (UserSession.getInstance().isLoggedIn() && durationMinutes > 0) {
                int userId = UserSession.getInstance().getCurrentUser().getId();
                String sessionType = isFocusSession ? "FOCUS" : "BREAK";

                // Record the session to database
                boolean success = focusSessionService.recordFocusSession(
                        userId,
                        durationMinutes,
                        associatedTaskId,
                        sessionType
                );

                if (success) {
                    System.out.println("📊 Recorded " + sessionType.toLowerCase() + " session: " + durationMinutes + " minutes");
                } else {
                    System.err.println("❌ Failed to record focus session to database");
                }
            }
        } catch (Exception e) {
            System.err.println("Error recording session: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateDisplay() {
        if (onTimeUpdate != null) {
            int minutes = currentSeconds / 60;
            int seconds = currentSeconds % 60;
            onTimeUpdate.accept(minutes, seconds);
        }
    }

    public double getProgress() {
        return 1.0 - ((double) currentSeconds / totalSeconds);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public boolean isFocusSession() {
        return isFocusSession;
    }

    public void setOnTimeUpdate(BiConsumer<Integer, Integer> onTimeUpdate) {
        this.onTimeUpdate = onTimeUpdate;
    }

    public void setOnTimerComplete(Runnable onTimerComplete) {
        this.onTimerComplete = onTimerComplete;
    }

    // NEW METHODS FOR ENHANCED FUNCTIONALITY

    /**
     * Associate this timer session with a specific task
     */
    public void setAssociatedTask(Integer taskId) {
        this.associatedTaskId = taskId;
        System.out.println("🎯 Timer associated with task ID: " + taskId);
    }

    /**
     * Clear task association
     */
    public void clearTaskAssociation() {
        this.associatedTaskId = null;
        System.out.println("🔄 Timer task association cleared");
    }

    /**
     * Get current session info
     */
    public SessionInfo getCurrentSessionInfo() {
        int elapsedSeconds = sessionStartSeconds - currentSeconds;
        return new SessionInfo(
                isFocusSession,
                isRunning,
                elapsedSeconds / 60, // elapsed minutes
                currentSeconds / 60, // remaining minutes
                associatedTaskId
        );
    }

    /**
     * Force complete current session (useful for manual completion)
     */
    public void forceComplete() {
        if (isRunning) {
            int elapsedSeconds = sessionStartSeconds - currentSeconds;
            if (elapsedSeconds >= 60) { // At least 1 minute
                recordSession(elapsedSeconds / 60);
            }

            complete();
        }
    }

    /**
     * Get today's total focus time for current user
     */
    public int getTodayFocusTime() {
        try {
            if (UserSession.getInstance().isLoggedIn()) {
                int userId = UserSession.getInstance().getCurrentUser().getId();
                return focusSessionService.getTodayFocusTime(userId);
            }
        } catch (Exception e) {
            System.err.println("Error getting today's focus time: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Set custom session durations
     */
    public void setCustomDurations(int focusMinutes, int breakMinutes) {
        // Only allow changes when timer is not running
        if (!isRunning) {
            // Update durations (convert to seconds)
            int newFocusDuration = focusMinutes * 60;
            int newBreakDuration = breakMinutes * 60;

            // Update current session if needed
            if (isFocusSession) {
                currentSeconds = newFocusDuration;
                totalSeconds = newFocusDuration;
            } else {
                currentSeconds = newBreakDuration;
                totalSeconds = newBreakDuration;
            }

            sessionStartSeconds = currentSeconds;
            updateDisplay();

            System.out.println("⚙️ Custom durations set - Focus: " + focusMinutes + "m, Break: " + breakMinutes + "m");
        }
    }

    /**
     * Get timer statistics
     */
    public TimerStatistics getStatistics() {
        try {
            if (UserSession.getInstance().isLoggedIn()) {
                int userId = UserSession.getInstance().getCurrentUser().getId();
                FocusSessionService.FocusSessionStats stats = focusSessionService.getFocusStats(userId, 7);

                return new TimerStatistics(
                        stats.sessionCount,
                        stats.totalMinutes,
                        stats.avgDuration,
                        focusSessionService.getFocusStreak(userId)
                );
            }
        } catch (Exception e) {
            System.err.println("Error getting timer statistics: " + e.getMessage());
        }

        return new TimerStatistics(0, 0, 0.0, 0);
    }

    // Inner classes for data structures
    public static class SessionInfo {
        public final boolean isFocusSession;
        public final boolean isRunning;
        public final int elapsedMinutes;
        public final int remainingMinutes;
        public final Integer associatedTaskId;

        public SessionInfo(boolean isFocusSession, boolean isRunning, int elapsedMinutes,
                           int remainingMinutes, Integer associatedTaskId) {
            this.isFocusSession = isFocusSession;
            this.isRunning = isRunning;
            this.elapsedMinutes = elapsedMinutes;
            this.remainingMinutes = remainingMinutes;
            this.associatedTaskId = associatedTaskId;
        }
    }

    public static class TimerStatistics {
        public final int totalSessions;
        public final int totalMinutes;
        public final double avgSessionDuration;
        public final int currentStreak;

        public TimerStatistics(int totalSessions, int totalMinutes, double avgSessionDuration, int currentStreak) {
            this.totalSessions = totalSessions;
            this.totalMinutes = totalMinutes;
            this.avgSessionDuration = avgSessionDuration;
            this.currentStreak = currentStreak;
        }
    }
}