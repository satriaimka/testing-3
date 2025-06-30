package com.focusbuddy.services;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.MoodEntry;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MoodService {
    
    public boolean saveMoodEntry(MoodEntry entry) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            // Check if entry for today already exists
            String checkQuery = "SELECT id FROM mood_entries WHERE user_id = ? AND entry_date = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setInt(1, entry.getUserId());
            checkStmt.setDate(2, Date.valueOf(entry.getEntryDate()));
            
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next()) {
                // Update existing entry
                String updateQuery = "UPDATE mood_entries SET mood_level = ?, mood_description = ? WHERE id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                updateStmt.setInt(1, entry.getMoodLevel());
                updateStmt.setString(2, entry.getMoodDescription());
                updateStmt.setInt(3, rs.getInt("id"));
                return updateStmt.executeUpdate() > 0;
            } else {
                // Insert new entry
                String insertQuery = "INSERT INTO mood_entries (user_id, mood_level, mood_description, entry_date) VALUES (?, ?, ?, ?)";
                PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                insertStmt.setInt(1, entry.getUserId());
                insertStmt.setInt(2, entry.getMoodLevel());
                insertStmt.setString(3, entry.getMoodDescription());
                insertStmt.setDate(4, Date.valueOf(entry.getEntryDate()));
                return insertStmt.executeUpdate() > 0;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public List<MoodEntry> getRecentMoodEntries(int userId, int days) {
        List<MoodEntry> entries = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM mood_entries WHERE user_id = ? AND entry_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) ORDER BY entry_date DESC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, days);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                MoodEntry entry = new MoodEntry();
                entry.setId(rs.getInt("id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setMoodLevel(rs.getInt("mood_level"));
                entry.setMoodDescription(rs.getString("mood_description"));
                entry.setEntryDate(rs.getDate("entry_date").toLocalDate());
                entries.add(entry);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return entries;
    }
    
    public List<MoodEntry> getMoodEntriesForChart(int userId, int days) {
        List<MoodEntry> entries = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM mood_entries WHERE user_id = ? AND entry_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) ORDER BY entry_date ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, days);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                MoodEntry entry = new MoodEntry();
                entry.setId(rs.getInt("id"));
                entry.setUserId(rs.getInt("user_id"));
                entry.setMoodLevel(rs.getInt("mood_level"));
                entry.setMoodDescription(rs.getString("mood_description"));
                entry.setEntryDate(rs.getDate("entry_date").toLocalDate());
                entries.add(entry);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return entries;
    }
    
    public int getMoodStreak(int userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = """
                SELECT COUNT(*) as streak FROM (
                    SELECT entry_date, 
                           ROW_NUMBER() OVER (ORDER BY entry_date DESC) as rn,
                           DATE_SUB(CURDATE(), INTERVAL ROW_NUMBER() OVER (ORDER BY entry_date DESC) - 1 DAY) as expected_date
                    FROM mood_entries 
                    WHERE user_id = ? AND entry_date <= CURDATE()
                    ORDER BY entry_date DESC
                ) t 
                WHERE entry_date = expected_date
                """;
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("streak");
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return 0;
    }
}
