package com.focusbuddy.utils;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;

public class SampleDataGenerator {
    
    public static void generateSampleData() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            
            // Create sample user if not exists
            String userQuery = "INSERT IGNORE INTO users (username, password, email, full_name) VALUES (?, ?, ?, ?)";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            userStmt.setString(1, "demo");
            userStmt.setString(2, "demo123");
            userStmt.setString(3, "demo@focusbuddy.com");
            userStmt.setString(4, "Demo User");
            userStmt.executeUpdate();
            
            // Create sample tasks
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
            
            String taskQuery = "INSERT IGNORE INTO tasks (user_id, title, description, priority, status, due_date) VALUES (1, ?, ?, ?, 'PENDING', ?)";
            
            for (int i = 0; i < sampleTasks.length; i++) {
                PreparedStatement taskStmt = conn.prepareStatement(taskQuery);
                taskStmt.setString(1, sampleTasks[i]);
                taskStmt.setString(2, descriptions[i]);
                taskStmt.setString(3, priorities[i].name());
                taskStmt.setDate(4, java.sql.Date.valueOf(LocalDate.now().plusDays(i + 1)));
                taskStmt.executeUpdate();
            }
            
            System.out.println("Sample data generated successfully!");
            
        } catch (Exception e) {
            System.err.println("Failed to generate sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
