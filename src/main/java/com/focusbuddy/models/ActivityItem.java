package com.focusbuddy.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ActivityItem {
    public enum ActivityType {
        TASK_CREATED, TASK_COMPLETED, TASK_UPDATED,
        MOOD_LOGGED,
        NOTE_CREATED, NOTE_UPDATED,
        GOAL_CREATED, GOAL_COMPLETED, GOAL_UPDATED
    }

    private int id;
    private ActivityType type;
    private String title;
    private String description;
    private String icon;
    private LocalDateTime timestamp;
    private int relatedId; // ID of the related object (task_id, note_id, etc.)

    public ActivityItem() {}

    public ActivityItem(ActivityType type, String title, String description, LocalDateTime timestamp) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.icon = getIconForType(type);
    }

    public ActivityItem(ActivityType type, String title, String description, LocalDateTime timestamp, int relatedId) {
        this(type, title, description, timestamp);
        this.relatedId = relatedId;
    }

    private String getIconForType(ActivityType type) {
        return switch (type) {
            case TASK_CREATED -> "📝";
            case TASK_COMPLETED -> "✅";
            case TASK_UPDATED -> "📋";
            case MOOD_LOGGED -> "😊";
            case NOTE_CREATED -> "📄";
            case NOTE_UPDATED -> "✏️";
            case GOAL_CREATED -> "🎯";
            case GOAL_COMPLETED -> "🏆";
            case GOAL_UPDATED -> "📈";
        };
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public ActivityType getType() { return type; }
    public void setType(ActivityType type) {
        this.type = type;
        this.icon = getIconForType(type);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getRelatedId() { return relatedId; }
    public void setRelatedId(int relatedId) { this.relatedId = relatedId; }

    // Utility methods
    public String getTimeAgo() {
        if (timestamp == null) return "Unknown time";

        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(timestamp, now);
        long hours = ChronoUnit.HOURS.between(timestamp, now);
        long days = ChronoUnit.DAYS.between(timestamp, now);

        if (minutes < 1) {
            return "Just now";
        } else if (minutes < 60) {
            return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        } else if (days < 7) {
            return days + " day" + (days == 1 ? "" : "s") + " ago";
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("MMM dd"));
        }
    }

    public String getFormattedTimestamp() {
        if (timestamp == null) return "";
        return timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
    }

    @Override
    public String toString() {
        return "ActivityItem{" +
                "type=" + type +
                ", title='" + title + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}