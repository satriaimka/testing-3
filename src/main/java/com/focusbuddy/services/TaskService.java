package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Task;
import com.focusbuddy.services.GoalProgressManager;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TaskService {

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

                // Handle created_at timestamp
                Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
                if (createdAtTimestamp != null) {
                    task.setCreatedAt(createdAtTimestamp.toLocalDateTime());
                }

                // Handle updated_at timestamp
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

                // ✅ NEW: Notify goal system about new task (for goal tracking)
                try {
                    // Could add goal tracking for "tasks created" if needed
                    // GoalProgressManager.getInstance().onTaskCreated(task.getUserId(), task);

                } catch (Exception e) {
                    System.err.println("⚠️ Error notifying goal system about new task: " + e.getMessage());
                }

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
            // Store old status untuk comparison
            Task.Status oldStatus = null;

            // Get current task dari database untuk compare status
            String selectQuery = "SELECT status FROM tasks WHERE id = ?";
            PreparedStatement selectStmt = conn.prepareStatement(selectQuery);
            selectStmt.setInt(1, task.getId());
            ResultSet rs = selectStmt.executeQuery();

            if (rs.next()) {
                oldStatus = Task.Status.valueOf(rs.getString("status"));
            }
            selectStmt.close();

            // Update task as usual
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

                // ✅ NEW: CHECK IF TASK WAS COMPLETED AND UPDATE GOALS
                if (oldStatus != Task.Status.COMPLETED &&
                        task.getStatus() == Task.Status.COMPLETED) {

                    System.out.println("🎯 Task completed! Updating related goals...");

                    try {
                        // Update goals otomatis ketika task completed
                        GoalProgressManager.getInstance().onTaskCompleted(task.getUserId(), task);

                    } catch (Exception e) {
                        System.err.println("⚠️ Error updating goals after task completion: " + e.getMessage());
                        // Jangan fail task update kalau goal update error
                    }
                }

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

    public List<Task> getTasksForToday(int userId) {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM tasks WHERE user_id = ? AND (due_date = ? OR due_date IS NULL) AND status != 'COMPLETED' ORDER BY priority DESC, created_at ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));

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

                // Handle timestamps
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
            System.err.println("Error getting today's tasks: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    public List<Task> getOverdueTasks(int userId) {
        List<Task> tasks = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM tasks WHERE user_id = ? AND due_date < ? AND status != 'COMPLETED' ORDER BY due_date ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setDate(2, Date.valueOf(LocalDate.now()));

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

                // Handle timestamps
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
            System.err.println("Error getting overdue tasks: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
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
            e.printStackTrace();
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
            e.printStackTrace();
        }

        return 0;
    }
}