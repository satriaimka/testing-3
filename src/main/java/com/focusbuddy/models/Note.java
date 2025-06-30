package com.focusbuddy.models;

import java.time.LocalDateTime;

public class Note {
    private int id;
    private int userId;
    private String title;
    private String content;
    private String tags;
    private String category;  // Field baru yang ditambahkan
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Note() {
        this.category = "General"; // Default category
    }

    public Note(String title, String content) {
        this();  // Memanggil constructor default untuk set category
        this.title = title;
        this.content = content;
    }

    // Constructor baru dengan category
    public Note(String title, String content, String category) {
        this.title = title;
        this.content = content;
        this.category = category != null ? category : "General";
    }

    // Getters and Setters yang sudah ada
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    // Getter and Setter baru untuk category
    public String getCategory() {
        return category != null ? category : "General";
    }

    public void setCategory(String category) {
        this.category = category != null ? category : "General";
    }

    // Method utility tambahan
    public boolean hasTag(String tag) {
        if (this.tags == null || tag == null) {
            return false;
        }
        return this.tags.toLowerCase().contains(tag.toLowerCase());
    }

    public String[] getTagsArray() {
        if (this.tags == null || this.tags.trim().isEmpty()) {
            return new String[0];
        }
        return this.tags.split(",");
    }

    // Override toString untuk debugging
    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", tags='" + tags + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    // Override equals dan hashCode untuk object comparison
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Note note = (Note) obj;
        return id == note.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}