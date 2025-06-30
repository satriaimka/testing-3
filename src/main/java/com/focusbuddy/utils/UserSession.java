package com.focusbuddy.utils;

import com.focusbuddy.models.User;

public class UserSession {
    private static UserSession instance;
    private User currentUser;
    
    private UserSession() {}
    
    public static synchronized UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    public void logout() {
        currentUser = null;
    }
}
