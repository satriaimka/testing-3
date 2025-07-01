package com.focusbuddy.services;

import com.focusbuddy.models.*;
import com.focusbuddy.database.DatabaseManager;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ActivityService {

    private TaskService taskService;
    private MoodService moodService;
    private NotesService notesService;
    private GoalsService goalsService;

    public ActivityService() {
        this.taskService = new TaskService();
        this.moodService = new MoodService();
        this.notesService = new NotesService();
        this.goalsService = new GoalsService();
    }

    /**
     * Get recent activities for a user
     * @param userId User ID
     * @param limit Maximum number of activities to return
     * @return List of recent activities sorted by timestamp (newest first)
     */
    public List<ActivityItem> getRecentActivities(int userId, int limit) {
        List<ActivityItem> activities = new ArrayList<>();

        try {
            // Get recent task activities
            activities.addAll(getRecentTaskActivities(userId));

            // Get recent mood activities
            activities.addAll(getRecentMoodActivities(userId));

            // Get recent note activities
            activities.addAll(getRecentNoteActivities(userId));

            // Get recent goal activities
            activities.addAll(getRecentGoalActivities(userId));

            // Sort by timestamp (newest first) and limit
            return activities.stream()
                    .sorted((a, b) -> {
                        if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                        if (a.getTimestamp() == null) return 1;
                        if (b.getTimestamp() == null) return -1;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error getting recent activities: " + e.getMessage());
            e.printStackTrace();
        }

        return activities;
    }

    private List<ActivityItem> getRecentTaskActivities(int userId) {
        List<ActivityItem> activities = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Get recent tasks (created and completed in last 7 days)
            String query = """
                SELECT id, title, status, created_at, updated_at 
                FROM tasks 
                WHERE user_id = ? 
                AND (created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) 
                     OR (status = 'COMPLETED' AND updated_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)))
                ORDER BY updated_at DESC 
                LIMIT 10
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                String status = rs.getString("status");
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                int taskId = rs.getInt("id");

                // Task completed activity
                if ("COMPLETED".equals(status) && updatedAt != null) {
                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.TASK_COMPLETED,
                            "Completed: " + title,
                            "Great job completing this task!",
                            updatedAt.toLocalDateTime(),
                            taskId
                    ));
                }

                // Task created activity (if created recently)
                if (createdAt != null && createdAt.toLocalDateTime().isAfter(LocalDateTime.now().minusDays(7))) {
                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.TASK_CREATED,
                            "Created task: " + title,
                            "New task added to your list",
                            createdAt.toLocalDateTime(),
                            taskId
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent task activities: " + e.getMessage());
        }

        return activities;
    }

    private List<ActivityItem> getRecentMoodActivities(int userId) {
        List<ActivityItem> activities = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Get recent mood entries (last 7 days)
            String query = """
                SELECT mood_level, mood_description, entry_date, created_at 
                FROM mood_entries 
                WHERE user_id = ? 
                AND entry_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
                ORDER BY entry_date DESC 
                LIMIT 5
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int moodLevel = rs.getInt("mood_level");
                String description = rs.getString("mood_description");
                Date entryDate = rs.getDate("entry_date");
                Timestamp createdAt = rs.getTimestamp("created_at");

                String moodEmoji = getMoodEmoji(moodLevel);
                String moodText = getMoodText(moodLevel);

                LocalDateTime timestamp = createdAt != null ?
                        createdAt.toLocalDateTime() :
                        entryDate.toLocalDate().atStartOfDay();

                String activityDescription = description != null && !description.trim().isEmpty() ?
                        description : "Mood tracked for the day";

                activities.add(new ActivityItem(
                        ActivityItem.ActivityType.MOOD_LOGGED,
                        "Logged mood: " + moodEmoji + " " + moodText,
                        activityDescription,
                        timestamp
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent mood activities: " + e.getMessage());
        }

        return activities;
    }

    private List<ActivityItem> getRecentNoteActivities(int userId) {
        List<ActivityItem> activities = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Get recent notes (created and updated in last 7 days)
            String query = """
                SELECT id, title, category, created_at, updated_at 
                FROM notes 
                WHERE user_id = ? 
                AND (created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) 
                     OR updated_at >= DATE_SUB(NOW(), INTERVAL 7 DAY))
                ORDER BY updated_at DESC 
                LIMIT 8
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                String category = rs.getString("category");
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                int noteId = rs.getInt("id");

                // Note updated activity (if updated recently and not the same as created)
                if (updatedAt != null && createdAt != null &&
                        !updatedAt.equals(createdAt) &&
                        updatedAt.toLocalDateTime().isAfter(LocalDateTime.now().minusDays(7))) {

                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.NOTE_UPDATED,
                            "Updated note: " + title,
                            "Note in " + (category != null ? category : "General") + " category",
                            updatedAt.toLocalDateTime(),
                            noteId
                    ));
                }

                // Note created activity
                if (createdAt != null && createdAt.toLocalDateTime().isAfter(LocalDateTime.now().minusDays(7))) {
                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.NOTE_CREATED,
                            "Created note: " + title,
                            "New note in " + (category != null ? category : "General"),
                            createdAt.toLocalDateTime(),
                            noteId
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent note activities: " + e.getMessage());
        }

        return activities;
    }

    private List<ActivityItem> getRecentGoalActivities(int userId) {
        List<ActivityItem> activities = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Get recent goals (created and completed in last 7 days)
            String query = """
                SELECT id, title, status, target_value, current_value, goal_type, created_at, updated_at 
                FROM goals 
                WHERE user_id = ? 
                AND (created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) 
                     OR (status = 'COMPLETED' AND updated_at >= DATE_SUB(NOW(), INTERVAL 7 DAY)))
                ORDER BY updated_at DESC 
                LIMIT 5
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String title = rs.getString("title");
                String status = rs.getString("status");
                String goalType = rs.getString("goal_type");
                int targetValue = rs.getInt("target_value");
                int currentValue = rs.getInt("current_value");
                Timestamp createdAt = rs.getTimestamp("created_at");
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                int goalId = rs.getInt("id");

                // Goal completed activity
                if ("COMPLETED".equals(status) && updatedAt != null) {
                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.GOAL_COMPLETED,
                            "🎉 Achieved goal: " + title,
                            "Congratulations on reaching your " + goalType.toLowerCase().replace("_", " ") + " goal!",
                            updatedAt.toLocalDateTime(),
                            goalId
                    ));
                }

                // Goal created activity
                if (createdAt != null && createdAt.toLocalDateTime().isAfter(LocalDateTime.now().minusDays(7))) {
                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.GOAL_CREATED,
                            "Set new goal: " + title,
                            "Target: " + targetValue + " " + goalType.toLowerCase().replace("_", " "),
                            createdAt.toLocalDateTime(),
                            goalId
                    ));
                }

                // Goal progress update (if significant progress made)
                if (currentValue > 0 && currentValue < targetValue && updatedAt != null &&
                        !updatedAt.equals(createdAt) &&
                        updatedAt.toLocalDateTime().isAfter(LocalDateTime.now().minusDays(7))) {

                    double progress = (double) currentValue / targetValue * 100;
                    activities.add(new ActivityItem(
                            ActivityItem.ActivityType.GOAL_UPDATED,
                            "Progress on: " + title,
                            String.format("%.0f%% complete (%d/%d)", progress, currentValue, targetValue),
                            updatedAt.toLocalDateTime(),
                            goalId
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent goal activities: " + e.getMessage());
        }

        return activities;
    }

    private String getMoodEmoji(int moodLevel) {
        return switch (moodLevel) {
            case 1 -> "😢";
            case 2 -> "😕";
            case 3 -> "😐";
            case 4 -> "😊";
            case 5 -> "😄";
            default -> "😐";
        };
    }

    private String getMoodText(int moodLevel) {
        return switch (moodLevel) {
            case 1 -> "Very Sad";
            case 2 -> "Sad";
            case 3 -> "Neutral";
            case 4 -> "Happy";
            case 5 -> "Very Happy";
            default -> "Neutral";
        };
    }

    /**
     * Get activity count for a user in the last N days
     * @param userId User ID
     * @param days Number of days to look back
     * @return Total number of activities
     */
    public int getActivityCount(int userId, int days) {
        return getRecentActivities(userId, Integer.MAX_VALUE).size();
    }

    /**
     * Get most active day for a user
     * @param userId User ID
     * @return String representing the most active day
     */
    public String getMostActiveDay(int userId) {
        // This could be implemented to analyze activity patterns
        // For now, return a simple message
        return "Keep tracking to see patterns!";
    }
}