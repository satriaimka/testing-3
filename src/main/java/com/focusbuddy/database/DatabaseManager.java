package com.focusbuddy.database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static DatabaseManager instance;
    private static final String URL = "jdbc:mysql://localhost:3306/focusbuddy?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    // Connection pool or single connection for tracking
    private Connection currentConnection;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        try {
            // Test if current connection is valid
            if (currentConnection != null && !currentConnection.isClosed() && currentConnection.isValid(5)) {
                return currentConnection;
            }
        } catch (SQLException e) {
            // Connection is not valid, create new one
        }

        // Create new connection
        currentConnection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
        return currentConnection;
    }

    // ✅ HAPUS METHOD cleanupAllData() - JANGAN BERSIHKAN DATA USER
    // Data user seharusnya tetap tersimpan di database

    public void initializeDatabase() {
        try (Connection conn = getConnection()) {
            createDatabase(conn);
            createTables(conn);
            runMigrations(conn);
            // ✅ TIDAK LAGI MEMANGGIL cleanupAllData() - BIARKAN DATA USER TETAP ADA
            System.out.println("Database initialized successfully!");
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createDatabase(Connection conn) throws SQLException {
        String createDbQuery = "CREATE DATABASE IF NOT EXISTS focusbuddy";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createDbQuery);
        }

        // Switch to the focusbuddy database
        String useDbQuery = "USE focusbuddy";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(useDbQuery);
        }
    }

    private void createTables(Connection conn) throws SQLException {
        String[] createTableQueries = {
                // Users table
                """
            CREATE TABLE IF NOT EXISTS users (
                id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) UNIQUE NOT NULL,
                password VARCHAR(255) NOT NULL,
                salt VARCHAR(255),
                email VARCHAR(100),
                full_name VARCHAR(100),
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                INDEX idx_username (username)
            )
            """,

                // Tasks table
                """
            CREATE TABLE IF NOT EXISTS tasks (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
                status ENUM('PENDING', 'IN_PROGRESS', 'COMPLETED') DEFAULT 'PENDING',
                due_date DATE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                INDEX idx_user_id (user_id),
                INDEX idx_status (status),
                INDEX idx_due_date (due_date)
            )
            """,

                // Mood entries table
                """
            CREATE TABLE IF NOT EXISTS mood_entries (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                mood_level INT CHECK (mood_level BETWEEN 1 AND 5),
                mood_description VARCHAR(500),
                entry_date DATE NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY unique_user_date (user_id, entry_date),
                INDEX idx_user_date (user_id, entry_date)
            )
            """,

                // Focus sessions table
                """
            CREATE TABLE IF NOT EXISTS focus_sessions (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                task_id INT,
                duration_minutes INT NOT NULL,
                session_date DATE NOT NULL,
                session_type ENUM('FOCUS', 'BREAK') DEFAULT 'FOCUS',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE SET NULL,
                INDEX idx_user_date (user_id, session_date)
            )
            """,

                // Notes table
                """
            CREATE TABLE IF NOT EXISTS notes (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                title VARCHAR(200) NOT NULL,
                content LONGTEXT,
                tags VARCHAR(500),
                category VARCHAR(100) DEFAULT 'General',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                INDEX idx_user_id (user_id),
                FULLTEXT KEY ft_title_content (title, content)
            )
            """,

                // Goals table
                """
            CREATE TABLE IF NOT EXISTS goals (
                id INT AUTO_INCREMENT PRIMARY KEY,
                user_id INT NOT NULL,
                title VARCHAR(200) NOT NULL,
                description TEXT,
                target_value INT NOT NULL,
                current_value INT DEFAULT 0,
                goal_type ENUM('STUDY_HOURS', 'TASKS_COMPLETED', 'FOCUS_SESSIONS') DEFAULT 'STUDY_HOURS',
                target_date DATE,
                status ENUM('ACTIVE', 'COMPLETED', 'PAUSED') DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                INDEX idx_user_status (user_id, status)
            )
            """,

                // Database migrations table
                """
            CREATE TABLE IF NOT EXISTS migrations (
                id INT AUTO_INCREMENT PRIMARY KEY,
                migration_name VARCHAR(255) NOT NULL,
                executed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_migration (migration_name)
            )
            """
        };

        for (String query : createTableQueries) {
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            }
        }
    }

    private void runMigrations(Connection conn) throws SQLException {
        // Migration 1: Add salt column to existing users table
        if (!migrationExists(conn, "add_salt_column")) {
            String addSaltColumn = "ALTER TABLE users ADD COLUMN IF NOT EXISTS salt VARCHAR(255)";
            try (PreparedStatement stmt = conn.prepareStatement(addSaltColumn)) {
                stmt.execute();
                recordMigration(conn, "add_salt_column");
            }
        }

        // Migration 2: Add updated_at to tasks table
        if (!migrationExists(conn, "add_updated_at_tasks")) {
            String addUpdatedAt = "ALTER TABLE tasks ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP";
            try (PreparedStatement stmt = conn.prepareStatement(addUpdatedAt)) {
                stmt.execute();
                recordMigration(conn, "add_updated_at_tasks");
            }
        }

        // Migration 3: Add category to notes table
        if (!migrationExists(conn, "add_category_notes")) {
            String addCategory = "ALTER TABLE notes ADD COLUMN IF NOT EXISTS category VARCHAR(100) DEFAULT 'General'";
            try (PreparedStatement stmt = conn.prepareStatement(addCategory)) {
                stmt.execute();
                recordMigration(conn, "add_category_notes");
            }
        }
    }

    private boolean migrationExists(Connection conn, String migrationName) throws SQLException {
        String query = "SELECT COUNT(*) FROM migrations WHERE migration_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, migrationName);
            ResultSet rs = stmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private void recordMigration(Connection conn, String migrationName) throws SQLException {
        String query = "INSERT INTO migrations (migration_name) VALUES (?)";
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, migrationName);
            stmt.executeUpdate();
        }
    }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // ✅ METODE BARU UNTUK DEBUGGING - HANYA UNTUK TESTING
    /**
     * Debugging method to check user data in database
     * HANYA UNTUK TESTING - JANGAN DIGUNAKAN DI PRODUCTION
     */
    public void debugUserData(int userId) {
        try (Connection conn = getConnection()) {
            String[] tables = {"tasks", "mood_entries", "focus_sessions", "notes", "goals"};

            System.out.println("=== DEBUG: User Data for ID: " + userId + " ===");

            for (String table : tables) {
                String query = "SELECT COUNT(*) as count FROM " + table + " WHERE user_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setInt(1, userId);
                    ResultSet rs = stmt.executeQuery();
                    if (rs.next()) {
                        int count = rs.getInt("count");
                        System.out.println(table + ": " + count + " records");
                    }
                }
            }
            System.out.println("=== END DEBUG ===");
        } catch (SQLException e) {
            System.err.println("Error debugging user data: " + e.getMessage());
        }
    }

    /**
     * Development method to reset ONLY demo data
     * Hanya menghapus data dari user demo, tidak user lain
     */
    public void resetDemoDataOnly() {
        try (Connection conn = getConnection()) {
            // Cari user demo
            String findDemoQuery = "SELECT id FROM users WHERE username = 'demo'";
            PreparedStatement findStmt = conn.prepareStatement(findDemoQuery);
            ResultSet rs = findStmt.executeQuery();

            if (rs.next()) {
                int demoUserId = rs.getInt("id");

                // Disable foreign key checks
                conn.createStatement().execute("SET FOREIGN_KEY_CHECKS = 0");

                // Delete only demo user data
                String[] tables = {"tasks", "mood_entries", "focus_sessions", "notes", "goals"};

                for (String table : tables) {
                    String deleteQuery = "DELETE FROM " + table + " WHERE user_id = ?";
                    PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                    deleteStmt.setInt(1, demoUserId);
                    int deleted = deleteStmt.executeUpdate();
                    System.out.println("✅ Deleted " + deleted + " records from " + table + " for demo user");
                    deleteStmt.close();
                }

                // Re-enable foreign key checks
                conn.createStatement().execute("SET FOREIGN_KEY_CHECKS = 1");

                System.out.println("✅ Demo data reset completed!");
            } else {
                System.out.println("ℹ️ Demo user not found, no data to reset");
            }

            findStmt.close();
        } catch (SQLException e) {
            System.err.println("Failed to reset demo data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // =============== ENHANCED CONNECTION MANAGEMENT ===============

    /**
     * Close all database connections properly
     * Called during application shutdown
     */
    public void closeConnections() {
        try {
            if (currentConnection != null && !currentConnection.isClosed()) {
                currentConnection.close();
                currentConnection = null;
                System.out.println("Database connection closed successfully.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Check if the current connection is valid
     * @return true if connection is valid, false otherwise
     */
    public boolean isConnectionValid() {
        try {
            return currentConnection != null &&
                    !currentConnection.isClosed() &&
                    currentConnection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Ensure we have a valid connection, reconnect if needed
     */
    public void ensureConnection() {
        try {
            if (!isConnectionValid()) {
                System.out.println("Reconnecting to database...");
                initializeDatabase();
            }
        } catch (Exception e) {
            System.err.println("Failed to ensure database connection: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get database connection statistics
     * @return String with connection info
     */
    public String getConnectionInfo() {
        try {
            if (currentConnection != null && !currentConnection.isClosed()) {
                DatabaseMetaData metaData = currentConnection.getMetaData();
                return String.format("Connected to: %s, Driver: %s, Version: %s",
                        metaData.getURL(),
                        metaData.getDriverName(),
                        metaData.getDriverVersion());
            } else {
                return "No active connection";
            }
        } catch (SQLException e) {
            return "Error getting connection info: " + e.getMessage();
        }
    }

    /**
     * Execute a simple health check query
     * @return true if database is responsive
     */
    public boolean healthCheck() {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT 1");
             ResultSet rs = stmt.executeQuery()) {

            return rs.next() && rs.getInt(1) == 1;
        } catch (SQLException e) {
            System.err.println("Database health check failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get the count of active connections (simplified version)
     * In a real connection pool, this would return actual pool stats
     * @return number of active connections
     */
    public int getActiveConnectionCount() {
        return (currentConnection != null && isConnectionValid()) ? 1 : 0;
    }

    /**
     * Force close and recreate the database connection
     */
    public void resetConnection() {
        closeConnections();
        ensureConnection();
    }
}