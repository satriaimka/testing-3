package com.focusbuddy.controllers;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.utils.ThemeManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class SplashController {

    @FXML private StackPane splashContainer;
    @FXML private Circle logoCircle1;
    @FXML private Circle logoCircle2;
    @FXML private Circle logoCircle3;
    @FXML private Label appTitle;
    @FXML private Rectangle progressBackground;
    @FXML private Rectangle progressBar;
    @FXML private Label loadingText;
    @FXML private Circle dot1;
    @FXML private Circle dot2;
    @FXML private Circle dot3;

    private Timeline logoAnimation;
    private Timeline dotsAnimation;
    private Timeline progressAnimation;

    @FXML
    private void initialize() {
        setupAnimations();
        startLoadingProcess();
    }

    private void setupAnimations() {
        // Logo circles rotation animation
        logoAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(logoCircle1.rotateProperty(), 0),
                        new KeyValue(logoCircle2.rotateProperty(), 0),
                        new KeyValue(logoCircle3.rotateProperty(), 0)
                ),
                new KeyFrame(Duration.seconds(2),
                        new KeyValue(logoCircle1.rotateProperty(), 360, Interpolator.EASE_BOTH),
                        new KeyValue(logoCircle2.rotateProperty(), -180, Interpolator.EASE_BOTH),
                        new KeyValue(logoCircle3.rotateProperty(), 360, Interpolator.EASE_BOTH)
                )
        );
        logoAnimation.setCycleCount(Timeline.INDEFINITE);

        // Loading dots animation
        dotsAnimation = createDotsAnimation();
        dotsAnimation.setCycleCount(Timeline.INDEFINITE);

        // App title fade in
        FadeTransition titleFade = new FadeTransition(Duration.seconds(1), appTitle);
        titleFade.setFromValue(0.0);
        titleFade.setToValue(1.0);
        titleFade.setInterpolator(Interpolator.EASE_OUT);

        // Start animations
        logoAnimation.play();
        dotsAnimation.play();
        titleFade.play();
    }

    private Timeline createDotsAnimation() {
        Timeline timeline = new Timeline();

        // Create pulsing effect for each dot with delay
        for (int i = 0; i < 3; i++) {
            Circle dot = (i == 0) ? dot1 : (i == 1) ? dot2 : dot3;

            KeyFrame fadeOut = new KeyFrame(Duration.seconds(i * 0.3),
                    new KeyValue(dot.opacityProperty(), 1.0));
            KeyFrame fadeIn = new KeyFrame(Duration.seconds(i * 0.3 + 0.5),
                    new KeyValue(dot.opacityProperty(), 0.3, Interpolator.EASE_BOTH));
            KeyFrame fadeOut2 = new KeyFrame(Duration.seconds(i * 0.3 + 1.0),
                    new KeyValue(dot.opacityProperty(), 1.0, Interpolator.EASE_BOTH));

            timeline.getKeyFrames().addAll(fadeOut, fadeIn, fadeOut2);
        }

        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(2))); // Total cycle duration
        return timeline;
    }

    private void startLoadingProcess() {
        Task<Void> loadingTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                // Step 1: Initialize Database
                updateProgress(0.1, 1.0);
                Platform.runLater(() -> loadingText.setText("Connecting to database..."));
                Thread.sleep(800);

                try {
                    DatabaseManager.getInstance().initializeDatabase();
                    updateProgress(0.3, 1.0);
                    Platform.runLater(() -> loadingText.setText("Database connected ✓"));
                } catch (Exception e) {
                    Platform.runLater(() -> loadingText.setText("Database connection failed"));
                    Thread.sleep(1000);
                }
                Thread.sleep(500);

                // Step 2: Load Configuration
                updateProgress(0.5, 1.0);
                Platform.runLater(() -> loadingText.setText("Loading configuration..."));
                Thread.sleep(600);

                // Step 3: Initialize Services
                updateProgress(0.7, 1.0);
                Platform.runLater(() -> loadingText.setText("Starting services..."));
                Thread.sleep(700);

                // Step 4: Preparing UI
                updateProgress(0.9, 1.0);
                Platform.runLater(() -> loadingText.setText("Preparing interface..."));
                Thread.sleep(500);

                // Step 5: Complete
                updateProgress(1.0, 1.0);
                Platform.runLater(() -> loadingText.setText("Ready to focus! 🚀"));
                Thread.sleep(800);

                return null;
            }

            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // Smooth transition to login
                    FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.5), splashContainer);
                    fadeOut.setFromValue(1.0);
                    fadeOut.setToValue(0.0);
                    fadeOut.setOnFinished(e -> showLoginScreen());
                    fadeOut.play();
                });
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    loadingText.setText("Loading failed. Please restart the application.");
                    loadingText.setStyle("-fx-text-fill: #ff6b6b;");
                });
            }
        };

        // Bind progress bar to task progress
        progressAnimation = new Timeline();
        loadingTask.progressProperty().addListener((obs, oldProgress, newProgress) -> {
            Platform.runLater(() -> {
                double targetWidth = 300.0 * newProgress.doubleValue();

                // Smooth progress bar animation
                Timeline progressTimeline = new Timeline(
                        new KeyFrame(Duration.seconds(0.3),
                                new KeyValue(progressBar.widthProperty(), targetWidth, Interpolator.EASE_OUT)
                        )
                );
                progressTimeline.play();
            });
        });

        Thread loadingThread = new Thread(loadingTask);
        loadingThread.setDaemon(true);
        loadingThread.start();
    }

    private void showLoginScreen() {
        try {
            // Stop all animations
            if (logoAnimation != null) logoAnimation.stop();
            if (dotsAnimation != null) dotsAnimation.stop();
            if (progressAnimation != null) progressAnimation.stop();

            // Get current stage (reuse the same stage - no style conflicts now)
            Stage currentStage = (Stage) splashContainer.getScene().getWindow();

            // Load login screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

            // Get screen dimensions for responsive design
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Create login scene
            Scene loginScene = new Scene(loader.load());

            // Apply theme
            ThemeManager.getInstance().applyTheme(loginScene, ThemeManager.getInstance().getCurrentTheme());

            // Configure current stage for login (NO style changes needed)
            currentStage.setScene(loginScene);
            currentStage.setTitle("FocusBuddy - Login");
            currentStage.setResizable(true); // Enable resizing

            // Set responsive size for login
            currentStage.setMinWidth(900);
            currentStage.setMinHeight(700);

            double loginWidth = Math.min(1200, screenBounds.getWidth() * 0.8);
            double loginHeight = Math.min(800, screenBounds.getHeight() * 0.85);

            currentStage.setWidth(loginWidth);
            currentStage.setHeight(loginHeight);

            // Center window
            currentStage.setX((screenBounds.getWidth() - loginWidth) / 2);
            currentStage.setY((screenBounds.getHeight() - loginHeight) / 2);

            // Ensure window is properly shown and focused
            currentStage.show();
            currentStage.toFront();
            currentStage.requestFocus();

            // Smooth fade in
            loginScene.getRoot().setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.5), loginScene.getRoot());
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to load login screen: " + e.getMessage());
        }
    }
}