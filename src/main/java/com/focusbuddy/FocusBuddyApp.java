package com.focusbuddy;

import com.focusbuddy.utils.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class FocusBuddyApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // Load splash screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/splash-screen.fxml"));

            // Get screen dimensions for centering
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Create splash scene with fixed size
            Scene splashScene = new Scene(loader.load(), 600, 400);

            // Apply modern light theme to splash
            splashScene.getStylesheets().clear();
            splashScene.getStylesheets().add(getClass().getResource("/css/modern-light-theme.css").toExternalForm());

            // Configure splash stage - KEEP DECORATED to avoid style conflicts
            primaryStage.setTitle("FocusBuddy - Loading...");
            primaryStage.setScene(splashScene);
            // DON'T SET UNDECORATED - this causes the style conflict
            primaryStage.setResizable(false);

            // Set fixed size for splash
            primaryStage.setWidth(600);
            primaryStage.setHeight(400);

            // Center splash screen
            primaryStage.setX((screenBounds.getWidth() - 600) / 2);
            primaryStage.setY((screenBounds.getHeight() - 400) / 2);

            // Set application icon (with error handling)
            try {
                Image icon = new Image(getClass().getResourceAsStream("/images/icon.png"));
                if (!icon.isError()) {
                    primaryStage.getIcons().add(icon);
                }
            } catch (Exception iconError) {
                System.out.println("Info: Application icon not found, using default.");
            }

            // Show splash screen
            primaryStage.show();

            // Set focus to ensure splash is visible
            primaryStage.toFront();
            primaryStage.requestFocus();

            // Handle window close request
            primaryStage.setOnCloseRequest(event -> {
                try {
                    System.exit(0);
                } catch (Exception e) {
                    System.err.println("Error during application shutdown: " + e.getMessage());
                    System.exit(1);
                }
            });

        } catch (Exception e) {
            System.err.println("Failed to start FocusBuddy application: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit("Failed to start application: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String errorMessage) {
        System.err.println("FATAL ERROR: " + errorMessage);
        System.err.println("The application will now exit.");
        System.exit(1);
    }

    public static void main(String[] args) {
        // Set system properties for better UI scaling and performance
        System.setProperty("prism.allowhidpi", "true");
        System.setProperty("glass.gtk.uiScale", "1.0");

        // Enable hardware acceleration if available
        System.setProperty("prism.order", "sw,d3d,es2");

        // Better font rendering
        System.setProperty("prism.text", "t2k");
        System.setProperty("prism.lcdtext", "true");

        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("Failed to launch application: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}