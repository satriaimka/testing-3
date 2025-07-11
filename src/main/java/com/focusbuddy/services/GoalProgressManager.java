package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Goal;
import com.focusbuddy.models.Task;
import com.focusbuddy.models.StudyGoal;
import com.focusbuddy.models.FocusGoal;
import com.focusbuddy.models.TaskGoal;
import com.focusbuddy.observers.GoalObserver;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 🎯 GOAL PROGRESS MANAGER - FIXED VERSION
 *
 * ✅ FIXED: Removed task completion handling to avoid double-update with database trigger
 * ✅ FOCUS: Only handles focus sessions (no database trigger conflict)
 * ✅ CLEAN: Simplified observer pattern for achievements only
 *
 * @author FocusBuddy Team
 * @version 2.1 - Fixed Double Update Bug
 */
public class GoalProgressManager {

    // ===================================================================
    // 🔧 SINGLETON SETUP & FIELDS
    // ===================================================================

    private static GoalProgressManager instance;
    private GoalsService goalsService;
    private List<GoalObserver> observers;

    private GoalProgressManager() {
        this.goalsService = new GoalsService();
        this.observers = new ArrayList<>();
    }

    public static synchronized GoalProgressManager getInstance() {
        if (instance == null) {
            instance = new GoalProgressManager();
        }
        return instance;
    }

    // ===================================================================
    // 🎯 OBSERVER MANAGEMENT
    // ===================================================================

    /**
     * Add observer untuk goal events
     */
    public void addObserver(GoalObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
            System.out.println("✅ Goal observer added: " + observer.getClass().getSimpleName());
        }
    }

    /**
     * Remove observer
     */
    public void removeObserver(GoalObserver observer) {
        if (observers.remove(observer)) {
            System.out.println("✅ Goal observer removed: " + observer.getClass().getSimpleName());
        }
    }

    // ===================================================================
    // 🚫 REMOVED: TASK COMPLETION HANDLING
    // ===================================================================

    /**
     * ❌ REMOVED: onTaskCompleted() method
     *
     * Reason: Database trigger 'update_goal_on_task_completion' already handles this
     * automatically. Manual updates from Java code caused double-increment bug.
     *
     * Database trigger handles:
     * - TASKS_COMPLETED goals update when task status changes to COMPLETED
     * - Automatic goal completion detection
     * - Status updates from ACTIVE to COMPLETED
     */

    // ===================================================================
    // 🍅 FOCUS SESSION HANDLING (NO CONFLICT)
    // ===================================================================

    /**
     * 🍅 AUTO-UPDATE: Ketika user menyelesaikan FOCUS SESSION
     * Method ini dipanggil dari PomodoroTimer.complete()
     *
     * @param userId ID user yang menyelesaikan focus session
     * @param durationMinutes Durasi focus session dalam menit
     */
    public void onFocusSessionCompleted(int userId, int durationMinutes) {
        try {
            System.out.println("🍅 Processing focus session completion for goal progress...");
            System.out.println("   Duration: " + durationMinutes + " minutes");
            System.out.println("   User ID: " + userId);

            // ✅ SAFE: Focus sessions don't have database triggers, so no conflict

            // Update FOCUS_SESSIONS goals (+1 session)
            updateFocusSessionGoals(userId);

            // Update STUDY_HOURS goals (convert minutes to hours)
            updateStudyHoursGoals(userId, durationMinutes);

        } catch (Exception e) {
            System.err.println("❌ Error processing focus session for goals: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================================================================
    // 🔧 GOAL UPDATE HELPERS (FOCUS SESSIONS ONLY)
    // ===================================================================

    /**
     * Update focus session goals
     */
    private void updateFocusSessionGoals(int userId) {
        try {
            List<Goal> focusSessionGoals = getActiveGoalsByType(userId, Goal.GoalType.FOCUS_SESSIONS);

            if (focusSessionGoals.isEmpty()) {
                System.out.println("   No active focus session goals found");
                return;
            }

            System.out.println("   Found " + focusSessionGoals.size() + " focus session goals to update");

            for (Goal goal : focusSessionGoals) {
                updateGoalProgress(goal, 1, "focus session completion");
            }

        } catch (Exception e) {
            System.err.println("❌ Error updating focus session goals: " + e.getMessage());
        }
    }

    /**
     * Update study hours goals based on focus session duration
     */
    private void updateStudyHoursGoals(int userId, int durationMinutes) {
        try {
            List<Goal> studyHoursGoals = getActiveGoalsByType(userId, Goal.GoalType.STUDY_HOURS);

            if (studyHoursGoals.isEmpty()) {
                System.out.println("   No active study hours goals found");
                return;
            }

            // Convert minutes to hours (count session >= 25 minutes as 1 hour)
            int hoursToAdd = (durationMinutes >= 25) ? 1 : 0;

            if (hoursToAdd > 0) {
                System.out.println("   Found " + studyHoursGoals.size() + " study hours goals to update");
                System.out.println("   Adding " + hoursToAdd + " hour(s) to each goal");

                for (Goal goal : studyHoursGoals) {
                    updateGoalProgress(goal, hoursToAdd, "study session (" + durationMinutes + " min)");
                }
            } else {
                System.out.println("   Focus session too short (" + durationMinutes + " min), not counting as study hour");
            }

        } catch (Exception e) {
            System.err.println("❌ Error updating study hours goals: " + e.getMessage());
        }
    }

    /**
     * Core method untuk update goal progress
     */
    private void updateGoalProgress(Goal goal, int increment, String reason) {
        try {
            int oldValue = goal.getCurrentValue();

            // Update progress menggunakan model method
            goal.updateProgress(increment);
            int newValue = goal.getCurrentValue();

            // Save ke database
            boolean saved = goalsService.updateGoal(goal);

            if (saved) {
                System.out.println("   ✅ Updated goal: \"" + goal.getTitle() + "\"");
                System.out.println("      Progress: " + oldValue + " → " + newValue + " (" + reason + ")");
                System.out.println("      Completion: " + String.format("%.1f%%", goal.getProgressPercentage()));

                // Check if goal completed
                if (goal.isCompleted() && goal.getStatus() == Goal.Status.COMPLETED) {
                    handleGoalCompletion(goal);
                } else {
                    // Notify progress update
                    notifyObserversProgressUpdated(goal, oldValue, newValue);
                }

            } else {
                System.err.println("   ❌ Failed to save goal progress to database");
                // Revert the progress update
                goal.setCurrentValue(oldValue);
            }

        } catch (Exception e) {
            System.err.println("❌ Error updating goal progress: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle goal completion
     */
    private void handleGoalCompletion(Goal goal) {
        try {
            System.out.println("🎉 GOAL COMPLETED: \"" + goal.getTitle() + "\"");

            // Show completion notification
            showGoalCompletionNotification(goal);

            // Notify observers
            notifyObserversGoalCompleted(goal);

        } catch (Exception e) {
            System.err.println("❌ Error handling goal completion: " + e.getMessage());
        }
    }

    // ===================================================================
    // 🔍 DATABASE QUERY HELPERS
    // ===================================================================

    /**
     * Get active goals by type for specific user
     */
    private List<Goal> getActiveGoalsByType(int userId, Goal.GoalType goalType) {
        List<Goal> activeGoals = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT * FROM goals 
                WHERE user_id = ? 
                AND goal_type = ? 
                AND status = 'ACTIVE'
                ORDER BY created_at DESC
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, goalType.name());

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Goal goal = createGoalFromResultSet(rs);
                activeGoals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error getting active goals by type: " + e.getMessage());
        }

        return activeGoals;
    }

    /**
     * Create Goal object from database ResultSet
     */
    private Goal createGoalFromResultSet(ResultSet rs) throws SQLException {
        Goal.GoalType type = Goal.GoalType.valueOf(rs.getString("goal_type"));

        // Create appropriate Goal subclass
        Goal goal = switch (type) {
            case STUDY_HOURS -> new StudyGoal();
            case FOCUS_SESSIONS -> new FocusGoal();
            case TASKS_COMPLETED -> new TaskGoal();
        };

        // Set properties from database
        goal.setId(rs.getInt("id"));
        goal.setUserId(rs.getInt("user_id"));
        goal.setTitle(rs.getString("title"));
        goal.setDescription(rs.getString("description"));
        goal.setTargetValue(rs.getInt("target_value"));
        goal.setCurrentValue(rs.getInt("current_value"));
        goal.setGoalType(type);
        goal.setTargetDate(rs.getDate("target_date").toLocalDate());
        goal.setStatus(Goal.Status.valueOf(rs.getString("status")));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            goal.setCreatedAt(createdAt.toLocalDateTime());
        }

        return goal;
    }

    // ===================================================================
    // 🎉 NOTIFICATIONS & OBSERVERS
    // ===================================================================

    /**
     * PUBLIC: Notify observers about goal creation
     * Dipanggil dari GoalsService.createGoal()
     */
    public void notifyGoalCreated(Goal goal) {
        try {
            System.out.println("📢 Notifying observers about goal creation: " + goal.getTitle());

            for (GoalObserver observer : observers) {
                try {
                    observer.onGoalCreated(goal);
                } catch (Exception e) {
                    System.err.println("❌ Error in observer.onGoalCreated: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error notifying goal created: " + e.getMessage());
        }
    }

    /**
     * PRIVATE: Notify observers about goal completion
     */
    private void notifyObserversGoalCompleted(Goal goal) {
        try {
            for (GoalObserver observer : observers) {
                try {
                    observer.onGoalCompleted(goal);
                } catch (Exception e) {
                    System.err.println("❌ Error in observer.onGoalCompleted: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error notifying goal completed: " + e.getMessage());
        }
    }

    /**
     * PRIVATE: Notify observers about goal progress update
     */
    private void notifyObserversProgressUpdated(Goal goal, int oldValue, int newValue) {
        try {
            for (GoalObserver observer : observers) {
                try {
                    observer.onGoalProgressUpdated(goal, oldValue, newValue);
                } catch (Exception e) {
                    System.err.println("❌ Error in observer.onGoalProgressUpdated: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error notifying goal progress updated: " + e.getMessage());
        }
    }

    /**
     * Show goal completion notification to user
     */
    private void showGoalCompletionNotification(Goal goal) {
        try {
            String title = "🏆 Achievement Unlocked!";
            String message = getGoalCompletionMessage(goal);

            NotificationManager.getInstance().showNotification(
                    title,
                    message,
                    NotificationManager.NotificationType.SUCCESS
            );

        } catch (Exception e) {
            System.err.println("❌ Error showing goal completion notification: " + e.getMessage());
        }
    }

    /**
     * Generate completion message based on goal type
     */
    private String getGoalCompletionMessage(Goal goal) {
        String baseMessage = String.format("🎉 GOAL ACHIEVED!\n\n\"%s\"\n\n", goal.getTitle());

        String typeMessage = switch (goal.getGoalType()) {
            case TASKS_COMPLETED -> String.format(
                    "✅ %d tasks completed! Your productivity is amazing!",
                    goal.getTargetValue()
            );
            case FOCUS_SESSIONS -> String.format(
                    "🍅 %d focus sessions completed! Your concentration is impressive!",
                    goal.getTargetValue()
            );
            case STUDY_HOURS -> String.format(
                    "📚 %d hours of focused study! Your dedication is inspiring!",
                    goal.getTargetValue()
            );
        };

        return baseMessage + typeMessage + "\n\n🚀 Keep up the excellent work!";
    }

    // ===================================================================
    // 📊 PUBLIC ANALYTICS METHODS
    // ===================================================================

    /**
     * Get goal completion rate untuk user
     */
    public double getGoalCompletionRate(int userId) {
        try {
            int totalGoals = goalsService.getTotalGoalsCount(userId);
            int completedGoals = goalsService.getCompletedGoalsCount(userId);

            return totalGoals > 0 ? (double) completedGoals / totalGoals * 100.0 : 0.0;

        } catch (Exception e) {
            System.err.println("❌ Error calculating goal completion rate: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get goals yang hampir selesai (>80% progress)
     */
    public List<Goal> getNearCompletionGoals(int userId) {
        List<Goal> nearCompletionGoals = new ArrayList<>();

        try {
            List<Goal> allGoals = goalsService.getGoalsForUser(userId);

            for (Goal goal : allGoals) {
                if (goal.getStatus() == Goal.Status.ACTIVE &&
                        goal.getProgressPercentage() >= 80.0) {
                    nearCompletionGoals.add(goal);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error getting near completion goals: " + e.getMessage());
        }

        return nearCompletionGoals;
    }

    /**
     * Get daily goal progress summary
     */
    public String getDailyGoalSummary(int userId) {
        try {
            List<Goal> activeGoals = new ArrayList<>();

            // Collect all active goals
            activeGoals.addAll(getActiveGoalsByType(userId, Goal.GoalType.TASKS_COMPLETED));
            activeGoals.addAll(getActiveGoalsByType(userId, Goal.GoalType.FOCUS_SESSIONS));
            activeGoals.addAll(getActiveGoalsByType(userId, Goal.GoalType.STUDY_HOURS));

            if (activeGoals.isEmpty()) {
                return "No active goals. Create your first goal to start tracking progress!";
            }

            int totalGoals = activeGoals.size();
            long nearCompletion = activeGoals.stream()
                    .filter(g -> g.getProgressPercentage() >= 80.0)
                    .count();

            return String.format(
                    "📊 Goal Progress: %d active goals, %d near completion (>80%%)",
                    totalGoals, nearCompletion
            );

        } catch (Exception e) {
            System.err.println("❌ Error getting daily goal summary: " + e.getMessage());
            return "Unable to load goal summary";
        }
    }

    // ===================================================================
    // 🛠️ UTILITY & MAINTENANCE METHODS
    // ===================================================================

    /**
     * Health check untuk goal progress system
     */
    public boolean healthCheck() {
        try {
            // Check if services are available
            if (goalsService == null) {
                System.err.println("❌ GoalsService is null");
                return false;
            }

            // Check if observers list is initialized
            if (observers == null) {
                System.err.println("❌ Observers list is null");
                return false;
            }

            System.out.println("✅ GoalProgressManager health check passed");
            System.out.println("   Active observers: " + observers.size());
            System.out.println("   Task completion handling: DISABLED (handled by database trigger)");
            System.out.println("   Focus session handling: ENABLED");

            return true;

        } catch (Exception e) {
            System.err.println("❌ GoalProgressManager health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Debug info untuk troubleshooting
     */
    public void printDebugInfo() {
        try {
            System.out.println("=== GoalProgressManager Debug Info (Fixed Version) ===");
            System.out.println("Instance: " + (instance != null ? "Initialized" : "Not initialized"));
            System.out.println("GoalsService: " + (goalsService != null ? "Available" : "Not available"));
            System.out.println("Observers count: " + (observers != null ? observers.size() : "null"));
            System.out.println("Task completion handling: DISABLED (database trigger handles this)");
            System.out.println("Focus session handling: ENABLED (no trigger conflict)");

            if (observers != null && !observers.isEmpty()) {
                System.out.println("Registered observers:");
                for (int i = 0; i < observers.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + observers.get(i).getClass().getSimpleName());
                }
            }

            System.out.println("======================================================");

        } catch (Exception e) {
            System.err.println("❌ Error printing debug info: " + e.getMessage());
        }
    }

    // ===================================================================
    // 🔧 MIGRATION HELPER (OPTIONAL)
    // ===================================================================

    /**
     * ✅ OPTIONAL: Method to fix existing goals with incorrect progress
     * Call this once to sync goals with actual task completion counts
     */
    public void syncTaskGoalsWithDatabase(int userId) {
        try {
            System.out.println("🔄 Syncing task completion goals with actual database state...");

            // Get actual completed task count
            try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                String taskQuery = "SELECT COUNT(*) as completed FROM tasks WHERE user_id = ? AND status = 'COMPLETED'";
                PreparedStatement taskStmt = conn.prepareStatement(taskQuery);
                taskStmt.setInt(1, userId);
                ResultSet taskRs = taskStmt.executeQuery();

                int actualCompletedTasks = 0;
                if (taskRs.next()) {
                    actualCompletedTasks = taskRs.getInt("completed");
                }

                System.out.println("📊 Actual completed tasks: " + actualCompletedTasks);

                // Update all task completion goals
                String updateQuery = """
                    UPDATE goals 
                    SET current_value = ?, 
                        status = CASE 
                            WHEN ? >= target_value THEN 'COMPLETED' 
                            ELSE 'ACTIVE' 
                        END
                    WHERE user_id = ? 
                    AND goal_type = 'TASKS_COMPLETED'
                    """;

                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setInt(1, actualCompletedTasks);
                updateStmt.setInt(2, actualCompletedTasks);
                updateStmt.setInt(3, userId);

                int updated = updateStmt.executeUpdate();
                System.out.println("✅ Updated " + updated + " task completion goals");

            }

        } catch (Exception e) {
            System.err.println("❌ Error syncing task goals: " + e.getMessage());
        }
    }
}