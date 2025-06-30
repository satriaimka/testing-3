package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Task;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskService {

    // Existing methods remain the same...
    public List<Task> getTasksForUser(int userId) {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM tasks WHERE user_id = ? ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Task task = new Task();
                task.setId(rs.getInt("id"));
                task.setUserId(rs.getInt("user_id"));
                task.setTitle(rs.getString("title"));
                task.setDescription(rs.getString("description"));
                task.setPriority(Task.Priority.valueOf(rs.getString("priority")));
                task.setStatus(Task.Status.valueOf(rs.getString("status")));

                Date dueDate = rs.getDate("due_date");
                if (dueDate != null) {
                    task.setDueDate(dueDate.toLocalDate());
                }

                Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                if (createdAtTimestamp != null) {
                    task.setCreatedAt(createdAtTimestamp.toLocalDateTime());
                }

                Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");
                if (updatedAtTimestamp != null) {
                    task.setUpdatedAt(updatedAtTimestamp.toLocalDateTime());
                }

                tasks.add(task);
            }

        } catch (SQLException e) {
            System.err.println("Error getting tasks for user: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    // NEW METHODS FOR ENHANCED DASHBOARD FUNCTIONALITY

    /**
     * Get tasks for a specific date
     */
    public List<Task> getTasksForDate(int userId, LocalDate date) {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM tasks WHERE user_id = ? AND (due_date = ? OR (due_date IS NULL AND DATE(created_at) = ?)) ORDER BY priority DESC, created_at ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(date));
            stmt.setDate(3, Date.valueOf(date));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Task task = createTaskFromResultSet(rs);
                tasks.add(task);
            }

        } catch (SQLException e) {
            System.err.println("Error getting tasks for date: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Get tasks for the last week
     */
    public List<Task> getTasksForLastWeek(int userId) {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            LocalDate weekAgo = LocalDate.now().minusDays(7);
            String query = "SELECT * FROM tasks WHERE user_id = ? AND created_at >= ? ORDER BY created_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(weekAgo.atStartOfDay()));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Task task = createTaskFromResultSet(rs);
                tasks.add(task);
            }

        } catch (SQLException e) {
            System.err.println("Error getting tasks for last week: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Get recently completed tasks
     */
    public List<Task> getRecentCompletedTasks(int userId, int limit) {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM tasks WHERE user_id = ? AND status = 'COMPLETED' ORDER BY updated_at DESC LIMIT ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Task task = createTaskFromResultSet(rs);
                tasks.add(task);
            }

        } catch (SQLException e) {
            System.err.println("Error getting recent completed tasks: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Get today's tasks (due today or created today)
     */
    public List<Task> getTasksForToday(int userId) {
        return getTasksForDate(userId, LocalDate.now());
    }

    /**
     * Get tasks completion rate for a period
     */
    public double getCompletionRate(int userId, int days) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            LocalDate startDate = LocalDate.now().minusDays(days);

            String query = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed
                FROM tasks 
                WHERE user_id = ? AND created_at >= ?
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(startDate.atStartOfDay()));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                int completed = rs.getInt("completed");

                if (total == 0) return 0.0;
                return (double) completed / total * 100.0;
            }

        } catch (SQLException e) {
            System.err.println("Error getting completion rate: " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Get task statistics for dashboard
     */
    public TaskStatistics getTaskStatistics(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT 
                    COUNT(*) as total,
                    SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
                    SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END) as pending,
                    SUM(CASE WHEN status = 'IN_PROGRESS' THEN 1 ELSE 0 END) as in_progress,
                    SUM(CASE WHEN due_date < CURDATE() AND status != 'COMPLETED' THEN 1 ELSE 0 END) as overdue
                FROM tasks 
                WHERE user_id = ?
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new TaskStatistics(
                        rs.getInt("total"),
                        rs.getInt("completed"),
                        rs.getInt("pending"),
                        rs.getInt("in_progress"),
                        rs.getInt("overdue")
                );
            }

        } catch (SQLException e) {
            System.err.println("Error getting task statistics: " + e.getMessage());
        }

        return new TaskStatistics(0, 0, 0, 0, 0);
    }

    /**
     * Get daily task completion data for charts
     */
    public List<DailyTaskData> getDailyTaskData(int userId, int days) {
        List<DailyTaskData> data = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT 
                    DATE(updated_at) as task_date,
                    COUNT(*) as completed_count
                FROM tasks 
                WHERE user_id = ? AND status = 'COMPLETED' AND updated_at >= ?
                GROUP BY DATE(updated_at)
                ORDER BY task_date ASC
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDate.now().minusDays(days).atStartOfDay()));

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Date taskDate = rs.getDate("task_date");
                int completedCount = rs.getInt("completed_count");

                if (taskDate != null) {
                    data.add(new DailyTaskData(taskDate.toLocalDate(), completedCount));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting daily task data: " + e.getMessage());
        }

        return data;
    }

    /**
     * Get task streak (consecutive days with completed tasks)
     */
    public int getTaskCompletionStreak(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            LocalDate currentDate = LocalDate.now();
            int streak = 0;

            for (int i = 0; i < 30; i++) { // Check last 30 days
                LocalDate checkDate = currentDate.minusDays(i);

                String query = "SELECT COUNT(*) FROM tasks WHERE user_id = ? AND DATE(updated_at) = ? AND status = 'COMPLETED'";
                PreparedStatement stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                stmt.setDate(2, Date.valueOf(checkDate));

                ResultSet rs = stmt.executeQuery();

                if (rs.next() && rs.getInt(1) > 0) {
                    streak++;
                } else if (i > 0) { // Don't break on today if no tasks completed yet
                    break;
                }

                stmt.close();
            }

            return streak;

        } catch (SQLException e) {
            System.err.println("Error calculating task completion streak: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Get average tasks completed per day
     */
    public double getAverageTasksPerDay(int userId, int days) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT COUNT(*) as total_completed
                FROM tasks 
                WHERE user_id = ? AND status = 'COMPLETED' AND updated_at >= ?
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDate.now().minusDays(days).atStartOfDay()));

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int totalCompleted = rs.getInt("total_completed");
                return (double) totalCompleted / days;
            }

        } catch (SQLException e) {
            System.err.println("Error getting average tasks per day: " + e.getMessage());
        }

        return 0.0;
    }

    /**
     * Get tasks by priority distribution
     */
    public PriorityDistribution getPriorityDistribution(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT 
                    priority,
                    COUNT(*) as count
                FROM tasks 
                WHERE user_id = ? 
                GROUP BY priority
                """;

            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            int high = 0, medium = 0, low = 0;

            while (rs.next()) {
                String priority = rs.getString("priority");
                int count = rs.getInt("count");

                switch (priority) {
                    case "HIGH" -> high = count;
                    case "MEDIUM" -> medium = count;
                    case "LOW" -> low = count;
                }
            }

            return new PriorityDistribution(high, medium, low);

        } catch (SQLException e) {
            System.err.println("Error getting priority distribution: " + e.getMessage());
        }

        return new PriorityDistribution(0, 0, 0);
    }

    // Existing methods continue here...
    public boolean addTask(Task task) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "INSERT INTO tasks (user_id, title, description, priority, status, due_date, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            stmt.setInt(1, task.getUserId());
            stmt.setString(2, task.getTitle());
            stmt.setString(3, task.getDescription());
            stmt.setString(4, task.getPriority().name());
            stmt.setString(5, task.getStatus().name());

            if (task.getDueDate() != null) {
                stmt.setDate(6, Date.valueOf(task.getDueDate()));
            } else {
                stmt.setNull(6, Types.DATE);
            }

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            stmt.setTimestamp(7, now);
            stmt.setTimestamp(8, now);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        task.setId(generatedKeys.getInt(1));
                    }
                }
                System.out.println("✅ Task added successfully: " + task.getTitle());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error adding task: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateTask(Task task) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, due_date = ?, updated_at = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getPriority().name());
            stmt.setString(4, task.getStatus().name());

            if (task.getDueDate() != null) {
                stmt.setDate(5, Date.valueOf(task.getDueDate()));
            } else {
                stmt.setNull(5, Types.DATE);
            }

            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(7, task.getId());

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Task updated successfully: " + task.getTitle());
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error updating task: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean deleteTask(int taskId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "DELETE FROM tasks WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, taskId);

            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("✅ Task deleted successfully, ID: " + taskId);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error deleting task: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public int getTaskCount(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) FROM tasks WHERE user_id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting task count: " + e.getMessage());
        }

        return 0;
    }

    public int getCompletedTaskCount(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) FROM tasks WHERE user_id = ? AND status = 'COMPLETED'";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error getting completed task count: " + e.getMessage());
        }

        return 0;
    }

    // Helper method to create Task from ResultSet
    private Task createTaskFromResultSet(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getInt("id"));
        task.setUserId(rs.getInt("user_id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setPriority(Task.Priority.valueOf(rs.getString("priority")));
        task.setStatus(Task.Status.valueOf(rs.getString("status")));

        Date dueDate = rs.getDate("due_date");
        if (dueDate != null) {
            task.setDueDate(dueDate.toLocalDate());
        }

        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            task.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }

        Timestamp updatedAtTimestamp = rs.getTimestamp("updated_at");
        if (updatedAtTimestamp != null) {
            task.setUpdatedAt(updatedAtTimestamp.toLocalDateTime());
        }

        return task;
    }

    // Inner classes for data structures
    public static class TaskStatistics {
        public final int total;
        public final int completed;
        public final int pending;
        public final int inProgress;
        public final int overdue;

        public TaskStatistics(int total, int completed, int pending, int inProgress, int overdue) {
            this.total = total;
            this.completed = completed;
            this.pending = pending;
            this.inProgress = inProgress;
            this.overdue = overdue;
        }
    }

    public static class DailyTaskData {
        public final LocalDate date;
        public final int completedCount;

        public DailyTaskData(LocalDate date, int completedCount) {
            this.date = date;
            this.completedCount = completedCount;
        }
    }

    public static class PriorityDistribution {
        public final int high;
        public final int medium;
        public final int low;

        public PriorityDistribution(int high, int medium, int low) {
            this.high = high;
            this.medium = medium;
            this.low = low;
        }
    }
}