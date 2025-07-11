package com.focusbuddy.models;

import java.time.LocalDateTime;

/**
 * STEP 1: Buat file RegularUser.java di package com.focusbuddy.models
 * File ini adalah implementasi konkret dari abstract class User
 */
public class RegularUser extends User {

    public RegularUser() {
        super();
    }

    public RegularUser(String username, String password, String email, String fullName) {
        super(username, password, email, fullName);
    }

    @Override
    public String getUserType() {
        return "REGULAR";
    }

    /**
     * Method untuk mendapatkan inisial user (untuk avatar)
     * Contoh: "John Doe" -> "JD", "John" -> "JO"
     */
    public String getInitials() {
        if (fullName == null || fullName.trim().isEmpty()) {
            return username != null ? username.substring(0, Math.min(2, username.length())).toUpperCase() : "U";
        }

        String[] names = fullName.trim().split("\\s+");
        if (names.length == 1) {
            return names[0].substring(0, Math.min(2, names[0].length())).toUpperCase();
        } else {
            return (names[0].charAt(0) + "" + names[names.length - 1].charAt(0)).toUpperCase();
        }
    }

    /**
     * Method untuk mendapatkan nama yang akan ditampilkan
     */
    public String getDisplayName() {
        return fullName != null && !fullName.trim().isEmpty() ? fullName : username;
    }

    /**
     * Method untuk format tanggal bergabung
     */
    public String getFormattedJoinDate() {
        if (createdAt == null) return "Recently joined";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime created = createdAt;

        long daysDiff = java.time.Duration.between(created, now).toDays();

        if (daysDiff == 0) {
            return "Joined today";
        } else if (daysDiff == 1) {
            return "Joined yesterday";
        } else if (daysDiff < 30) {
            return "Joined " + daysDiff + " days ago";
        } else if (daysDiff < 365) {
            long months = daysDiff / 30;
            return "Joined " + months + " month" + (months > 1 ? "s" : "") + " ago";
        } else {
            long years = daysDiff / 365;
            return "Joined " + years + " year" + (years > 1 ? "s" : "") + " ago";
        }
    }
}