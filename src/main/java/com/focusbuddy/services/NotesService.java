package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Note;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotesService {

    public boolean createNote(Note note) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Updated query to include category field
            String query = "INSERT INTO notes (user_id, title, content, tags, category, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            LocalDateTime now = LocalDateTime.now();

            stmt.setInt(1, note.getUserId());
            stmt.setString(2, note.getTitle());
            stmt.setString(3, note.getContent());
            stmt.setString(4, note.getTags());
            stmt.setString(5, note.getCategory() != null ? note.getCategory() : "General"); // Handle category
            stmt.setTimestamp(6, Timestamp.valueOf(now));
            stmt.setTimestamp(7, Timestamp.valueOf(now));

            int result = stmt.executeUpdate();

            if (result > 0) {
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    note.setId(keys.getInt(1));
                }
                // Set timestamps
                note.setCreatedAt(now);
                note.setUpdatedAt(now);
                return true;
            }

        } catch (SQLException e) {
            System.err.println("Error creating note: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    public boolean updateNote(Note note) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Updated query to include category field
            String query = "UPDATE notes SET title = ?, content = ?, tags = ?, category = ?, updated_at = ? WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);

            LocalDateTime now = LocalDateTime.now();

            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setString(3, note.getTags());
            stmt.setString(4, note.getCategory() != null ? note.getCategory() : "General"); // Handle category
            stmt.setTimestamp(5, Timestamp.valueOf(now));
            stmt.setInt(6, note.getId());

            boolean success = stmt.executeUpdate() > 0;
            if (success) {
                note.setUpdatedAt(now); // Update timestamp in object
            }

            return success;

        } catch (SQLException e) {
            System.err.println("Error updating note: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteNote(int noteId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "DELETE FROM notes WHERE id = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, noteId);

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error deleting note: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public List<Note> getNotesForUser(int userId) {
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Updated query to include category field
            String query = "SELECT id, user_id, title, content, tags, category, created_at, updated_at FROM notes WHERE user_id = ? ORDER BY updated_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Note note = new Note();
                note.setId(rs.getInt("id"));
                note.setUserId(rs.getInt("user_id"));
                note.setTitle(rs.getString("title"));
                note.setContent(rs.getString("content"));
                note.setTags(rs.getString("tags"));
                note.setCategory(rs.getString("category")); // Handle category

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    note.setCreatedAt(createdAt.toLocalDateTime());
                }

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    note.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                notes.add(note);
            }

        } catch (SQLException e) {
            System.err.println("Error getting notes for user: " + e.getMessage());
            e.printStackTrace();
        }

        return notes;
    }

    public List<Note> searchNotes(int userId, String searchText) {
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Updated query to include category field in search and select
            String query = "SELECT id, user_id, title, content, tags, category, created_at, updated_at " +
                    "FROM notes WHERE user_id = ? AND " +
                    "(LOWER(title) LIKE LOWER(?) OR LOWER(content) LIKE LOWER(?) OR LOWER(tags) LIKE LOWER(?) OR LOWER(category) LIKE LOWER(?)) " +
                    "ORDER BY updated_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);

            String searchPattern = "%" + searchText + "%";
            stmt.setInt(1, userId);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            stmt.setString(4, searchPattern);
            stmt.setString(5, searchPattern); // Search in category field too

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Note note = new Note();
                note.setId(rs.getInt("id"));
                note.setUserId(rs.getInt("user_id"));
                note.setTitle(rs.getString("title"));
                note.setContent(rs.getString("content"));
                note.setTags(rs.getString("tags"));
                note.setCategory(rs.getString("category")); // Handle category

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    note.setCreatedAt(createdAt.toLocalDateTime());
                }

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    note.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                notes.add(note);
            }

        } catch (SQLException e) {
            System.err.println("Error searching notes: " + e.getMessage());
            e.printStackTrace();
        }

        return notes;
    }

    // New method: Get notes by category
    public List<Note> getNotesByCategory(int userId, String category) {
        List<Note> notes = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT id, user_id, title, content, tags, category, created_at, updated_at " +
                    "FROM notes WHERE user_id = ? AND category = ? ORDER BY updated_at DESC";
            PreparedStatement stmt = conn.prepareStatement(query);

            stmt.setInt(1, userId);
            stmt.setString(2, category);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Note note = new Note();
                note.setId(rs.getInt("id"));
                note.setUserId(rs.getInt("user_id"));
                note.setTitle(rs.getString("title"));
                note.setContent(rs.getString("content"));
                note.setTags(rs.getString("tags"));
                note.setCategory(rs.getString("category"));

                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    note.setCreatedAt(createdAt.toLocalDateTime());
                }

                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    note.setUpdatedAt(updatedAt.toLocalDateTime());
                }

                notes.add(note);
            }

        } catch (SQLException e) {
            System.err.println("Error getting notes by category: " + e.getMessage());
            e.printStackTrace();
        }

        return notes;
    }

    // New method: Get all categories for a user
    public List<String> getCategoriesForUser(int userId) {
        List<String> categories = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT DISTINCT category FROM notes WHERE user_id = ? AND category IS NOT NULL ORDER BY category";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String category = rs.getString("category");
                if (category != null && !category.trim().isEmpty()) {
                    categories.add(category);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error getting categories for user: " + e.getMessage());
            e.printStackTrace();
        }

        return categories;
    }

    // New method: Get note count by category
    public int getNoteCountByCategory(int userId, String category) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT COUNT(*) as count FROM notes WHERE user_id = ? AND category = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setString(2, category);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("count");
            }

        } catch (SQLException e) {
            System.err.println("Error getting note count by category: " + e.getMessage());
            e.printStackTrace();
        }

        return 0;
    }

    // New method: Migrate existing notes to have default category (utility method)
    public boolean migrateNotesToDefaultCategory() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "UPDATE notes SET category = 'General' WHERE category IS NULL OR category = ''";
            PreparedStatement stmt = conn.prepareStatement(query);

            int updated = stmt.executeUpdate();
            System.out.println("✅ Migrated " + updated + " notes to default category 'General'");

            return true;

        } catch (SQLException e) {
            System.err.println("Error migrating notes to default category: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}