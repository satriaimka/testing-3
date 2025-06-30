package com.focusbuddy.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MoodEntry {
    private int id;
    private int userId;
    private int moodLevel; // 1-5 scale
    private String moodDescription;
    private LocalDate entryDate;
    private LocalDateTime createdAt;
    
    public MoodEntry() {
        this.entryDate = LocalDate.now();
    }
    
    public MoodEntry(int userId, int moodLevel, String moodDescription) {
        this();
        this.userId = userId;
        this.moodLevel = moodLevel;
        this.moodDescription = moodDescription;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getMoodLevel() { return moodLevel; }
    public void setMoodLevel(int moodLevel) { this.moodLevel = moodLevel; }
    
    public String getMoodDescription() { return moodDescription; }
    public void setMoodDescription(String moodDescription) { this.moodDescription = moodDescription; }
    
    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getMoodEmoji() {
        switch (moodLevel) {
            case 1: return "😢";
            case 2: return "😕";
            case 3: return "😐";
            case 4: return "😊";
            case 5: return "😄";
            default: return "😐";
        }
    }
}
