package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Goal;
import com.focusbuddy.models.StudyGoal;
import com.focusbuddy.models.FocusGoal;
import com.focusbuddy.models.TaskGoal;
import com.focusbuddy.services.GoalProgressManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GoalsService {

    public boolean createGoal(Goal goal) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "INSERT INTO goals (user_id, title, description, target_value, goal_type, target_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, goal.getUserId());
            stmt.setString(2, goal.getTitle());
            stmt.setString(3, goal.getDescription());
            stmt.setInt(4, goal.getTargetValue());
            stmt.setString(5, goal.getGoalType().name());
            stmt.setDate(6, Date.valueOf(goal.getTargetDate()));
            stmt.setString(7, goal.getStatus().name());

            int result = stmt.executeUpdate();

            if (result > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    goal.setId(keys.getInt(1));
                }

                System.out.println("✅ Goal created successfully: " + goal.getTitle());

                // ✅ NEW: Notify observers about goal creation
                try {
                    GoalProgressManager.getInstance().notifyGoalCreated(goal);

                } catch (Exception e) {
                    System.err.println("⚠️ Error notifying goal creation: " + e.getMessage());
                    // Don't fail goal creation if notification fails
                }

                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error creating goal: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }
    
    public boolean updateGoal(Goal goal) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "UPDATE goals SET title = ?, description = ?, target_value = ?, current_value = ?, target_date = ?, status = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            
            stmt.setString(1, goal.getTitle());
            stmt.setString(2, goal.getDescription());
            stmt.setInt(3, goal.getTargetValue());
            stmt.setInt(4, goal.getCurrentValue());
            stmt.setDate(5, Date.valueOf(goal.getTargetDate()));
            stmt.setString(6, goal.getStatus().name());
            stmt.setInt(7, goal.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean deleteGoal(int goalId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "DELETE FROM goals WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, goalId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Goal> getGoalsForUser(int userId) {
        List<Goal> goals = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM goals WHERE user_id = ? ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                Goal goal = createGoalFromResultSet(rs);
                goals.add(goal);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return goals;
    }

    private Goal createGoalFromResultSet(ResultSet rs) throws SQLException {
        Goal.GoalType type = Goal.GoalType.valueOf(rs.getString("goal_type"));

        // ✅ UPDATED: Create appropriate Goal subclass dengan TaskGoal yang baru
        Goal goal = switch (type) {
            case STUDY_HOURS -> new StudyGoal();
            case FOCUS_SESSIONS -> new FocusGoal();
            case TASKS_COMPLETED -> new TaskGoal(); // ✅ GUNAKAN TaskGoal YANG BARU
        };

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


    public int getTotalGoalsCount(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) FROM goals WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public int getCompletedGoalsCount(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) FROM goals WHERE user_id = ? AND status = 'COMPLETED'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public int getActiveGoalsCount(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) FROM goals WHERE user_id = ? AND status = 'ACTIVE'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    public List<String> getUserAchievements(int userId) {
        List<String> achievements = new ArrayList<>();
        
        int completedGoals = getCompletedGoalsCount(userId);
        int totalGoals = getTotalGoalsCount(userId);
        
        // Achievement logic
        if (completedGoals >= 1) {
            achievements.add("First Goal Completed!");
        }
        if (completedGoals >= 5) {
            achievements.add("Goal Achiever - 5 Goals Completed");
        }
        if (completedGoals >= 10) {
            achievements.add("Goal Master - 10 Goals Completed");
        }
        if (totalGoals >= 20) {
            achievements.add("Goal Setter - Created 20+ Goals");
        }
        
        // Check for streak achievements
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) FROM goals WHERE user_id = ? AND status = 'COMPLETED' AND target_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt(1) >= 3) {
                achievements.add("Weekly Warrior - 3+ Goals This Week");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return achievements;
    }
    
    public boolean updateGoalProgress(int goalId, int increment) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "UPDATE goals SET current_value = current_value + ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, increment);
            stmt.setInt(2, goalId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Goal> getUpcomingDeadlineGoals(int userId) {
        List<Goal> goals = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
            SELECT * FROM goals 
            WHERE user_id = ? 
            AND status = 'ACTIVE'
            AND target_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 3 DAY)
            ORDER BY target_date ASC
            """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Goal goal = createGoalFromResultSet(rs);
                goals.add(goal);
            }

        } catch (SQLException e) {
            System.err.println("Error getting upcoming deadline goals: " + e.getMessage());
            e.printStackTrace();
        }

        return goals;
    }

    public List<Goal> getGoalsByProgressRange(int userId, double minProgress, double maxProgress) {
        List<Goal> filteredGoals = new ArrayList<>();

        try {
            List<Goal> allGoals = getGoalsForUser(userId);

            for (Goal goal : allGoals) {
                double progress = goal.getProgressPercentage();
                if (progress >= minProgress && progress <= maxProgress &&
                        goal.getStatus() == Goal.Status.ACTIVE) {
                    filteredGoals.add(goal);
                }
            }

        } catch (Exception e) {
            System.err.println("Error filtering goals by progress: " + e.getMessage());
        }

        return filteredGoals;
    }

    public GoalStats getDailyGoalStats(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Today's goal progress
            String todayQuery = """
            SELECT 
                COUNT(*) as total_active,
                SUM(CASE WHEN current_value >= target_value THEN 1 ELSE 0 END) as completed_today,
                AVG(CASE WHEN target_value > 0 THEN (current_value * 100.0 / target_value) ELSE 0 END) as avg_progress
            FROM goals 
            WHERE user_id = ? 
            AND status = 'ACTIVE'
            """;

            PreparedStatement stmt = conn.prepareStatement(todayQuery);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int totalActive = rs.getInt("total_active");
                int completedToday = rs.getInt("completed_today");
                double avgProgress = rs.getDouble("avg_progress");

                return new GoalStats(totalActive, completedToday, avgProgress);
            }

        } catch (SQLException e) {
            System.err.println("Error getting daily goal stats: " + e.getMessage());
        }

        return new GoalStats(0, 0, 0.0);
    }

    /**
     * Helper class untuk goal statistics
     */
    public static class GoalStats {
        public final int totalActive;
        public final int completedToday;
        public final double averageProgress;

        public GoalStats(int totalActive, int completedToday, double averageProgress) {
            this.totalActive = totalActive;
            this.completedToday = completedToday;
            this.averageProgress = averageProgress;
        }

        public String getProgressSummary() {
            if (totalActive == 0) {
                return "No active goals. Time to set some targets!";
            }

            return String.format(
                    "%d active goals, %.0f%% average progress",
                    totalActive, averageProgress
            );
        }

        public boolean hasActiveGoals() {
            return totalActive > 0;
        }

        public boolean hasCompletedGoalsToday() {
            return completedToday > 0;
        }
    }
}
