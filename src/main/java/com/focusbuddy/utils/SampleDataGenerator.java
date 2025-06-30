package com.focusbuddy.utils;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Task;

import java.sql.*;
import java.time.LocalDate;

public class SampleDataGenerator {
    
    private static void cleanupOrphanedData(Connection conn, int demoUserId) throws SQLException {
        // Tables to clean up
        String[] tables = {"tasks", "mood_entries", "focus_sessions", "notes", "goals"};
        
        for (String table : tables) {
            // Find orphaned records
            String findOrphanedQuery = String.format("""
                SELECT t.id 
                FROM %s t 
                LEFT JOIN users u ON t.user_id = u.id 
                WHERE u.id IS NULL OR t.user_id = 1
                """, table);
            
            PreparedStatement findStmt = conn.prepareStatement(findOrphanedQuery);
            ResultSet orphanedRecords = findStmt.executeQuery();
            
            // Update orphaned records to belong to demo user
            if (orphanedRecords.next()) {
                String updateQuery = String.format("UPDATE %s SET user_id = ? WHERE id = ?", table);
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                
                int count = 0;
                do {
                    updateStmt.setInt(1, demoUserId);
                    updateStmt.setInt(2, orphanedRecords.getInt("id"));
                    updateStmt.executeUpdate();
                    count++;
                } while (orphanedRecords.next());
                
                System.out.printf("✅ %d orphaned %s have been reassigned to demo user%n", 
                    count, table);
            }
        }
    }

    public static void generateSampleData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // First, check if demo user exists and get their ID
            String checkDemoQuery = "SELECT id FROM users WHERE username = 'demo'";
            PreparedStatement checkStmt = conn.prepareStatement(checkDemoQuery);
            ResultSet rs = checkStmt.executeQuery();
            
            int demoUserId;
            if (!rs.next()) {
                // Create demo user if not exists
                String userQuery = "INSERT INTO users (username, password, email, full_name) VALUES (?, ?, ?, ?)";
                PreparedStatement userStmt = conn.prepareStatement(userQuery, Statement.RETURN_GENERATED_KEYS);
                userStmt.setString(1, "demo");
                userStmt.setString(2, "demo123");
                userStmt.setString(3, "demo@focusbuddy.com");
                userStmt.setString(4, "Demo User");
                userStmt.executeUpdate();
                
                ResultSet generatedKeys = userStmt.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    throw new SQLException("Failed to create demo user");
                }
                demoUserId = generatedKeys.getInt(1);
            } else {
                demoUserId = rs.getInt("id");
            }
            
            // Clean up any orphaned data and assign them to demo user
            cleanupOrphanedData(conn, demoUserId);
            
            // Check if demo user already has sample tasks
            String checkTasksQuery = "SELECT COUNT(*) FROM tasks WHERE user_id = ?";
            PreparedStatement checkTasksStmt = conn.prepareStatement(checkTasksQuery);
            checkTasksStmt.setInt(1, demoUserId);
            ResultSet tasksRs = checkTasksStmt.executeQuery();
            
            // Only generate sample tasks if demo user has no tasks
            if (tasksRs.next() && tasksRs.getInt(1) == 0) {
                // Create sample tasks only for demo user
                String[] sampleTasks = {
                "Complete Java OOP Assignment",
                "Study for Database Exam", 
                "Prepare Presentation Slides",
                "Review Design Patterns",
                "Practice Coding Problems"
            };
            
            String[] descriptions = {
                "Implement inheritance and polymorphism concepts",
                "Focus on SQL queries and normalization",
                "Create slides for software engineering project",
                "Study Singleton, Factory, and Observer patterns",
                "Solve algorithmic challenges on coding platforms"
            };
            
            Task.Priority[] priorities = {
                Task.Priority.HIGH, Task.Priority.MEDIUM, Task.Priority.HIGH,
                Task.Priority.LOW, Task.Priority.MEDIUM
            };
            
                String taskQuery = "INSERT INTO tasks (user_id, title, description, priority, status, due_date) VALUES (?, ?, ?, ?, 'PENDING', ?)";
                
                for (int i = 0; i < sampleTasks.length; i++) {
                    PreparedStatement taskStmt = conn.prepareStatement(taskQuery);
                    taskStmt.setInt(1, demoUserId);
                    taskStmt.setString(2, sampleTasks[i]);
                    taskStmt.setString(3, descriptions[i]);
                    taskStmt.setString(4, priorities[i].name());
                    taskStmt.setDate(5, java.sql.Date.valueOf(LocalDate.now().plusDays(i + 1)));
                    taskStmt.executeUpdate();
                }
                
                System.out.println("✅ Sample data generated successfully for demo user!");
            } else {
                System.out.println("ℹ️ Demo user already has tasks, skipping sample data generation.");
            }
            
        } catch (Exception e) {
            System.err.println("Failed to generate sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
