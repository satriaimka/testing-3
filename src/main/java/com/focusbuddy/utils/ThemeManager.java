package com.focusbuddy.utils;

import javafx.scene.Scene;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme = Theme.LIGHT;

    public enum Theme {
        LIGHT("modern-light-theme.css"),
        DARK("modern-dark-theme.css");

        private final String cssFile;

        Theme(String cssFile) {
            this.cssFile = cssFile;
        }

        public String getCssFile() {
            return cssFile;
        }
    }

    private ThemeManager() {}

    public static synchronized ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void applyTheme(Scene scene, Theme theme) {
        if (scene == null) {
            System.err.println("Cannot apply theme: Scene is null");
            return;
        }

        try {
            // Clear existing stylesheets
            scene.getStylesheets().clear();

            // Load the new theme CSS
            String cssPath = "/css/" + theme.getCssFile();
            String cssUrl = getClass().getResource(cssPath).toExternalForm();
            scene.getStylesheets().add(cssUrl);

            // Update current theme
            currentTheme = theme;

            System.out.println("Applied theme: " + theme.name() + " (" + theme.getCssFile() + ")");

        } catch (Exception e) {
            System.err.println("Failed to apply theme " + theme.name() + ": " + e.getMessage());
            e.printStackTrace();

            // Fallback to default JavaFX theme
            scene.getStylesheets().clear();
        }
    }

    public void toggleTheme(Scene scene) {
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyThemeWithAnimation(scene, newTheme);
    }

    public void applyThemeWithAnimation(Scene scene, Theme theme) {
        if (scene == null || scene.getRoot() == null) {
            applyTheme(scene, theme);
            return;
        }

        try {
            // Create fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), scene.getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.85);

            fadeOut.setOnFinished(e -> {
                // Apply new theme
                applyTheme(scene, theme);

                // Create fade in animation
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), scene.getRoot());
                fadeIn.setFromValue(0.85);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });

            fadeOut.play();

        } catch (Exception e) {
            System.err.println("Animation failed, applying theme directly: " + e.getMessage());
            applyTheme(scene, theme);
        }
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setCurrentTheme(Theme theme) {
        this.currentTheme = theme;
    }

    public boolean isDarkTheme() {
        return currentTheme == Theme.DARK;
    }

    public boolean isLightTheme() {
        return currentTheme == Theme.LIGHT;
    }

    /**
     * Get the opposite theme of the current one
     * @return opposite theme
     */
    public Theme getOppositeTheme() {
        return (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
    }

    /**
     * Apply theme based on system preference (if available)
     * For now, defaults to light theme
     */
    public void applySystemTheme(Scene scene) {
        // In a real implementation, you might check system dark mode preference
        // For now, we'll default to light theme
        applyTheme(scene, Theme.LIGHT);
    }

    /**
     * Get theme description for UI display
     * @return user-friendly theme name
     */
    public String getCurrentThemeDisplayName() {
        return switch (currentTheme) {
            case LIGHT -> "Light Mode";
            case DARK -> "Dark Mode";
        };
    }

    /**
     * Get theme icon for UI display
     * @return emoji icon representing current theme
     */
    public String getCurrentThemeIcon() {
        return switch (currentTheme) {
            case LIGHT -> "☀️";
            case DARK -> "🌙";
        };
    }

    /**
     * Get next theme icon (for toggle button)
     * @return emoji icon for the theme that will be applied on toggle
     */
    public String getNextThemeIcon() {
        return switch (currentTheme) {
            case LIGHT -> "🌙";
            case DARK -> "☀️";
        };
    }

    /**
     * Save theme preference (stub for future persistence implementation)
     */
    public void saveThemePreference() {
        // TODO: Implement saving to preferences file or database
        System.out.println("Theme preference saved: " + currentTheme.name());
    }

    /**
     * Load theme preference (stub for future persistence implementation)
     */
    public Theme loadThemePreference() {
        // TODO: Implement loading from preferences file or database
        // For now, return default light theme
        return Theme.LIGHT;
    }

    /**
     * Reset to default theme
     */
    public void resetToDefault(Scene scene) {
        applyTheme(scene, Theme.LIGHT);
    }

    /**
     * Check if CSS file exists for a theme
     * @param theme theme to check
     * @return true if CSS file exists
     */
    public boolean isThemeAvailable(Theme theme) {
        try {
            String cssPath = "/css/" + theme.getCssFile();
            return getClass().getResource(cssPath) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get all available themes
     * @return array of available themes
     */
    public Theme[] getAvailableThemes() {
        return Theme.values();
    }
}