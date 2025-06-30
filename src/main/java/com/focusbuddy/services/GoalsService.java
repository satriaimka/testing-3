package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Goal;
import com.focusbuddy.models.StudyGoal;
import com.focusbuddy.models.FocusGoal;

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
                return true;
            }
            
        } catch (SQLException e) {
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
        
        Goal goal = switch (type) {
            case STUDY_HOURS -> new StudyGoal();
            case FOCUS_SESSIONS -> new FocusGoal();
            case TASKS_COMPLETED -> new StudyGoal(); // Can create TaskGoal class
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
}
