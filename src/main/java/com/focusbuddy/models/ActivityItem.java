package com.focusbuddy.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ActivityItem {

    public enum ActivityType {
        TASK_CREATED("📝", "Task Created"),
        TASK_COMPLETED("✅", "Task Completed"),
        TASK_UPDATED("🔄", "Task Updated"),
        NOTE_CREATED("📄", "Note Created"),
        NOTE_UPDATED("✏️", "Note Updated"),
        MOOD_LOGGED("😊", "Mood Logged"),
        GOAL_CREATED("🎯", "Goal Created"),
        GOAL_COMPLETED("🏆", "Goal Achieved"),
        GOAL_UPDATED("📈", "Goal Progress"),
        FOCUS_SESSION("🍅", "Focus Session"); // ✅ NEW: Focus session type

        private final String icon;
        private final String displayName;

        ActivityType(String icon, String displayName) {
            this.icon = icon;
            this.displayName = displayName;
        }

        public String getIcon() {
            return icon;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private ActivityType type;
    private String title;
    private String description;
    private LocalDateTime timestamp;
    private Integer relatedId; // ID of related entity (task, note, etc.)

    // Constructors
    public ActivityItem() {}

    public ActivityItem(ActivityType type, String title, String description, LocalDateTime timestamp) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
    }

    public ActivityItem(ActivityType type, String title, String description, LocalDateTime timestamp, Integer relatedId) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.relatedId = relatedId;
    }

    // Getters and Setters
    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Integer relatedId) {
        this.relatedId = relatedId;
    }

    // Helper methods
    public String getIcon() {
        return type != null ? type.getIcon() : "📋";
    }

    public String getTimeAgo() {
        if (timestamp == null) {
            return "Unknown time";
        }

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (days < 7) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        } else {
            // For older items, show date
            return timestamp.format(DateTimeFormatter.ofPattern("MMM d"));
        }
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "Unknown";
        }
        return timestamp.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
    }

    public boolean isToday() {
        if (timestamp == null) {
            return false;
        }
        return timestamp.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    public boolean isThisWeek() {
        if (timestamp == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
        return timestamp.isAfter(weekAgo);
    }

    @Override
    public String toString() {
        return "ActivityItem{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                ", relatedId=" + relatedId +
                '}';
    }
}