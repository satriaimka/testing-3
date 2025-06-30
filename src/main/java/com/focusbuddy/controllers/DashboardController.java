package com.focusbuddy.controllers;

import com.focusbuddy.models.Task;
import com.focusbuddy.services.PomodoroTimer;
import com.focusbuddy.services.TaskService;
import com.focusbuddy.utils.ThemeManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.ErrorHandler;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DashboardController {

    @FXML private BorderPane dashboardContainer;
    @FXML private VBox sidebar;
    @FXML private StackPane contentArea;
    @FXML private Label welcomeLabel;
    @FXML private ToggleButton themeToggle;
    @FXML private Button logoutButton;

    // Navigation buttons
    @FXML private Button dashboardBtn;
    @FXML private Button tasksBtn;
    @FXML private Button pomodoroBtn;
    @FXML private Button moodBtn;
    @FXML private Button notesBtn;
    @FXML private Button goalsBtn;
    @FXML private Button exportBtn;

    // Dashboard content elements
    @FXML private Label timerDisplay;
    @FXML private Button startTimerBtn;
    @FXML private Button pauseTimerBtn;
    @FXML private Button resetTimerBtn;
    @FXML private ProgressBar timerProgress;
    @FXML private VBox tasksList;
    @FXML private Label tasksCompletedLabel;
    @FXML private Label focusTimeLabel;

    private PomodoroTimer pomodoroTimer;
    private TaskService taskService;
    private String currentView = "dashboard";
    private Node dashboardContent;

    @FXML
    private void initialize() {
        try {
            // Initialize services safely
            initializeServices();

            // Set welcome message with error handling
            setupWelcomeMessage();

            // Store original dashboard content
            if (contentArea != null && contentArea.getChildren().size() > 0) {
                dashboardContent = contentArea.getChildren().get(0);
            }

            // Set up navigation with error handling
            setupNavigationSafely();

            // Set up theme toggle with error handling
            setupThemeToggleSafely();

            // Set up timer (only if dashboard elements exist)
            setupPomodoroTimerSafely();

            // Set up logout with error handling
            setupLogoutSafely();

            // Setup responsive layout AFTER window is shown
            Platform.runLater(() -> {
                try {
                    setupResponsiveLayout();
                    ensureWindowIsMaximized();
                    loadDashboardDataSafely();
                    startPeriodicUpdates();
                } catch (Exception e) {
                    ErrorHandler.handleError("Dashboard Initialization",
                            "Failed to complete dashboard setup", e);
                }
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Dashboard Initialization Error",
                    "Failed to initialize dashboard", e);
        }
    }

    private void initializeServices() {
        try {
            taskService = new TaskService();
            pomodoroTimer = new PomodoroTimer();
            System.out.println("✅ Services initialized successfully");
        } catch (Exception e) {
            ErrorHandler.handleError("Service Initialization",
                    "Failed to initialize dashboard services", e);
            // Fallback: create empty instances
            if (taskService == null) taskService = new TaskService();
            if (pomodoroTimer == null) pomodoroTimer = new PomodoroTimer();
        }
    }

    private void setupWelcomeMessage() {
        try {
            if (welcomeLabel != null && UserSession.getInstance().isLoggedIn()) {
                String userName = UserSession.getInstance().getCurrentUser().getFullName();
                String greeting = getGreetingByTime();
                welcomeLabel.setText(greeting + ", " + userName + "!");

                // Add welcome animation
                animateWelcomeMessage();
            }
        } catch (Exception e) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Welcome to FocusBuddy!");
            }
            System.err.println("Warning: Could not set personalized welcome message: " + e.getMessage());
        }
    }

    private String getGreetingByTime() {
        LocalTime now = LocalTime.now();
        if (now.isBefore(LocalTime.of(12, 0))) {
            return "Good morning";
        } else if (now.isBefore(LocalTime.of(17, 0))) {
            return "Good afternoon";
        } else {
            return "Good evening";
        }
    }

    private void animateWelcomeMessage() {
        if (welcomeLabel == null) return;

        try {
            welcomeLabel.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), welcomeLabel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.setInterpolator(Interpolator.EASE_OUT);
            fadeIn.play();
        } catch (Exception e) {
            welcomeLabel.setOpacity(1); // Fallback
        }
    }

    private void ensureWindowIsMaximized() {
        try {
            Stage stage = (Stage) dashboardContainer.getScene().getWindow();

            if (stage != null) {
                // Force window to be resizable and maximized
                stage.setResizable(true);

                // Set minimum constraints
                stage.setMinWidth(1200);
                stage.setMinHeight(800);

                // Get screen bounds
                Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

                // If not already maximized, maximize it
                if (!stage.isMaximized()) {
                    stage.setMaximized(true);
                }

                // Double-check: if still not maximized, set manually
                Platform.runLater(() -> {
                    try {
                        if (!stage.isMaximized()) {
                            stage.setX(0);
                            stage.setY(0);
                            stage.setWidth(screenBounds.getWidth());
                            stage.setHeight(screenBounds.getHeight());
                        }

                        // Ensure content fills the window
                        if (dashboardContainer != null) {
                            dashboardContainer.setPrefWidth(stage.getWidth());
                            dashboardContainer.setPrefHeight(stage.getHeight());
                        }
                    } catch (Exception e) {
                        System.err.println("Error in delayed window setup: " + e.getMessage());
                    }
                });

                System.out.println("✅ Window maximized: " + stage.isMaximized());
                System.out.println("✅ Window size: " + stage.getWidth() + "x" + stage.getHeight());
            }
        } catch (Exception e) {
            ErrorHandler.handleError("Window Setup", "Error ensuring window is maximized", e);
        }
    }

    private void setupResponsiveLayout() {
        try {
            Stage stage = (Stage) dashboardContainer.getScene().getWindow();

            if (stage != null) {
                // Listen for window resize events
                stage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                    Platform.runLater(() -> adjustLayoutForSize(newWidth.doubleValue(), stage.getHeight()));
                });

                stage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                    Platform.runLater(() -> adjustLayoutForSize(stage.getWidth(), newHeight.doubleValue()));
                });

                // Initial layout adjustment
                adjustLayoutForSize(stage.getWidth(), stage.getHeight());
            }
        } catch (Exception e) {
            ErrorHandler.handleError("Responsive Layout", "Error setting up responsive layout", e);
        }
    }

    private void adjustLayoutForSize(double width, double height) {
        try {
            // Adjust sidebar width based on screen size
            if (sidebar != null) {
                if (width < 1400) {
                    sidebar.setPrefWidth(260);
                    sidebar.setMinWidth(240);
                } else {
                    sidebar.setPrefWidth(280);
                    sidebar.setMinWidth(260);
                }
            }

            // Adjust content area
            if (contentArea != null) {
                contentArea.setPrefWidth(width - (sidebar != null ? sidebar.getWidth() : 280));
                contentArea.setPrefHeight(height);
            }

            // Adjust dashboard container
            if (dashboardContainer != null) {
                dashboardContainer.setPrefWidth(width);
                dashboardContainer.setPrefHeight(height);
            }

            System.out.println("✅ Layout adjusted for size: " + width + "x" + height);
        } catch (Exception e) {
            System.err.println("Error adjusting layout: " + e.getMessage());
        }
    }

    private void setupNavigationSafely() {
        try {
            if (dashboardBtn != null) dashboardBtn.setOnAction(e -> showDashboard());
            if (tasksBtn != null) tasksBtn.setOnAction(e -> showTasks());
            if (pomodoroBtn != null) pomodoroBtn.setOnAction(e -> showPomodoro());
            if (moodBtn != null) moodBtn.setOnAction(e -> showMoodTracker());
            if (notesBtn != null) notesBtn.setOnAction(e -> showNotes());
            if (goalsBtn != null) goalsBtn.setOnAction(e -> showGoals());
            if (exportBtn != null) exportBtn.setOnAction(e -> showExport());

            // Set initial active button
            setActiveButton(dashboardBtn);

            // Add hover animations to navigation buttons
            addNavigationAnimations();

            System.out.println("✅ Navigation setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Navigation Setup", "Failed to setup navigation", e);
        }
    }

    private void addNavigationAnimations() {
        Button[] navButtons = {dashboardBtn, tasksBtn, pomodoroBtn, moodBtn, notesBtn, goalsBtn, exportBtn};

        for (Button button : navButtons) {
            if (button != null) {
                addControlHoverEffect(button);
            }
        }
    }

    // FIXED: Changed parameter from Button to Control to accept both Button and ToggleButton
    private void addControlHoverEffect(Control control) {
        try {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), control);
            scaleUp.setToX(1.03);
            scaleUp.setToY(1.03);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), control);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            control.setOnMouseEntered(e -> scaleUp.play());
            control.setOnMouseExited(e -> scaleDown.play());
        } catch (Exception e) {
            System.err.println("Warning: Could not add hover effect to control: " + e.getMessage());
        }
    }

    // Keep the old method for backward compatibility with Button parameters
    private void addButtonHoverEffect(Button button) {
        addControlHoverEffect(button);
    }

    private void setupThemeToggleSafely() {
        try {
            if (themeToggle != null) {
                // Set initial theme toggle state
                ThemeManager themeManager = ThemeManager.getInstance();
                themeToggle.setText(themeManager.getCurrentThemeIcon());
                themeToggle.setSelected(themeManager.isDarkTheme());

                themeToggle.setOnAction(e -> toggleThemeWithAnimation());

                // FIXED: Use addControlHoverEffect instead of addButtonHoverEffect
                addControlHoverEffect(themeToggle);

                System.out.println("✅ Theme toggle setup completed");
            }
        } catch (Exception e) {
            ErrorHandler.handleError("Theme Setup", "Failed to setup theme toggle", e);
        }
    }

    private void toggleThemeWithAnimation() {
        try {
            Scene scene = dashboardContainer.getScene();
            ThemeManager themeManager = ThemeManager.getInstance();

            // Create smooth transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), dashboardContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.85);

            fadeOut.setOnFinished(e -> {
                try {
                    themeManager.toggleTheme(scene);
                    themeToggle.setText(themeManager.getCurrentThemeIcon());

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(200), dashboardContainer);
                    fadeIn.setFromValue(0.85);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                } catch (Exception ex) {
                    ErrorHandler.handleError("Theme Toggle", "Failed to toggle theme", ex);
                }
            });

            fadeOut.play();
        } catch (Exception e) {
            // Fallback without animation
            ThemeManager.getInstance().toggleTheme(dashboardContainer.getScene());
            if (themeToggle != null) {
                themeToggle.setText(ThemeManager.getInstance().getCurrentThemeIcon());
            }
        }
    }

    private void setupPomodoroTimerSafely() {
        if (timerDisplay == null || pomodoroTimer == null) {
            System.out.println("⚠️ Timer elements not found, skipping timer setup");
            return;
        }

        try {
            // Set up timer display update
            pomodoroTimer.setOnTimeUpdate((minutes, seconds) -> {
                Platform.runLater(() -> {
                    try {
                        if (timerDisplay != null) {
                            timerDisplay.setText(String.format("%02d:%02d", minutes, seconds));
                        }
                        if (timerProgress != null) {
                            double progress = pomodoroTimer.getProgress();
                            timerProgress.setProgress(progress);

                            // Add pulsing animation when timer is running
                            if (pomodoroTimer.isRunning() && progress > 0) {
                                addTimerPulseAnimation();
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error updating timer display: " + e.getMessage());
                    }
                });
            });

            // Set up timer completion
            pomodoroTimer.setOnTimerComplete(() -> {
                Platform.runLater(() -> {
                    try {
                        String sessionType = pomodoroTimer.isFocusSession() ? "Focus" : "Break";
                        NotificationManager.getInstance().showNotification(
                                sessionType + " Session Complete!",
                                "Great job! " + (pomodoroTimer.isFocusSession() ?
                                        "Time for a well-deserved break." : "Ready for another focus session?"),
                                NotificationManager.NotificationType.SUCCESS
                        );

                        // Add completion animation
                        addTimerCompletionAnimation();

                        if (resetTimerBtn != null) {
                            resetTimerBtn.fire();
                        }
                    } catch (Exception e) {
                        System.err.println("Error handling timer completion: " + e.getMessage());
                    }
                });
            });

            // Set up timer controls with error handling
            setupTimerControls();

            // Initialize display
            if (timerDisplay != null) {
                timerDisplay.setText("25:00");
            }
            if (timerProgress != null) {
                timerProgress.setProgress(0);
            }

            System.out.println("✅ Pomodoro timer setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Timer Setup", "Failed to setup Pomodoro timer", e);
        }
    }

    private void setupTimerControls() {
        try {
            if (startTimerBtn != null) {
                startTimerBtn.setOnAction(e -> {
                    try {
                        pomodoroTimer.start();
                        showTimerFeedback("Timer started! Stay focused. 🎯");
                    } catch (Exception ex) {
                        ErrorHandler.handleError("Timer Start", "Failed to start timer", ex);
                    }
                });
                addButtonHoverEffect(startTimerBtn);
            }

            if (pauseTimerBtn != null) {
                pauseTimerBtn.setOnAction(e -> {
                    try {
                        pomodoroTimer.pause();
                        showTimerFeedback("Timer paused. Take a moment to breathe. 😌");
                    } catch (Exception ex) {
                        ErrorHandler.handleError("Timer Pause", "Failed to pause timer", ex);
                    }
                });
                addButtonHoverEffect(pauseTimerBtn);
            }

            if (resetTimerBtn != null) {
                resetTimerBtn.setOnAction(e -> {
                    try {
                        pomodoroTimer.reset();
                        if (timerDisplay != null) {
                            timerDisplay.setText("25:00");
                        }
                        if (timerProgress != null) {
                            timerProgress.setProgress(0);
                        }
                        showTimerFeedback("Timer reset. Ready for a fresh start! ✨");
                    } catch (Exception ex) {
                        ErrorHandler.handleError("Timer Reset", "Failed to reset timer", ex);
                    }
                });
                addButtonHoverEffect(resetTimerBtn);
            }
        } catch (Exception e) {
            System.err.println("Error setting up timer controls: " + e.getMessage());
        }
    }

    private void addTimerPulseAnimation() {
        if (timerDisplay == null) return;

        try {
            ScaleTransition pulse = new ScaleTransition(Duration.seconds(1), timerDisplay);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.05);
            pulse.setToY(1.05);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.play();
        } catch (Exception e) {
            System.err.println("Error adding timer pulse animation: " + e.getMessage());
        }
    }

    private void addTimerCompletionAnimation() {
        if (timerDisplay == null) return;

        try {
            // Flash animation
            FadeTransition flash = new FadeTransition(Duration.millis(200), timerDisplay);
            flash.setFromValue(1.0);
            flash.setToValue(0.3);
            flash.setCycleCount(6);
            flash.setAutoReverse(true);
            flash.play();
        } catch (Exception e) {
            System.err.println("Error adding timer completion animation: " + e.getMessage());
        }
    }

    private void showTimerFeedback(String message) {
        try {
            NotificationManager.getInstance().showNotification(
                    "Timer", message, NotificationManager.NotificationType.INFO
            );
        } catch (Exception e) {
            System.err.println("Error showing timer feedback: " + e.getMessage());
        }
    }

    private void setupLogoutSafely() {
        try {
            if (logoutButton != null) {
                logoutButton.setOnAction(e -> handleLogout());
                addButtonHoverEffect(logoutButton);
                System.out.println("✅ Logout button setup completed");
            }
        } catch (Exception e) {
            ErrorHandler.handleError("Logout Setup", "Failed to setup logout button", e);
        }
    }

    @FXML private Label productivityLabel;
    @FXML private Label streakLabel;
    @FXML private Label goalsProgressLabel;
    @FXML private Label moodAverageLabel;

    private void loadDashboardDataSafely() {
        if (tasksList == null || taskService == null) {
            System.out.println("⚠️ Tasks list or service not available, skipping tasks load");
            return;
        }

        try {
            int userId = UserSession.getInstance().getCurrentUser().getId();

            // Load data asynchronously to avoid blocking UI
            CompletableFuture.supplyAsync(() -> {
                try {
                    return taskService.getTasksForUser(userId);
                } catch (Exception e) {
                    System.err.println("Error loading tasks: " + e.getMessage());
                    return List.<Task>of(); // Return empty list as fallback
                }
            }).thenAccept(todayTasks -> {
                Platform.runLater(() -> {
                    try {
                        updateTasksList(todayTasks);
                        updateStatistics(todayTasks);
                        
                        // Update productivity insights with empty state for new users
                        if (productivityLabel != null) {
                            productivityLabel.setText("Start tasks to see efficiency");
                        }
                        if (streakLabel != null) {
                            streakLabel.setText("0 days");
                        }
                        if (goalsProgressLabel != null) {
                            goalsProgressLabel.setText("No goals set");
                        }
                        if (moodAverageLabel != null) {
                            moodAverageLabel.setText("No mood entries");
                        }
                        
                        System.out.println("✅ Dashboard data loaded successfully");
                    } catch (Exception e) {
                        System.err.println("Error updating dashboard data: " + e.getMessage());
                    }
                });
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Data Loading", "Failed to load dashboard data", e);
        }
    }

    private void updateTasksList(List<Task> tasks) {
        if (tasksList == null) return;

        try {
            tasksList.getChildren().clear();

            // Show only first 5 tasks or empty state
            if (tasks.isEmpty()) {
                Label emptyLabel = new Label("No tasks yet. Create your first task to get started!");
                emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
                tasksList.getChildren().add(emptyLabel);
            } else {
                int taskCount = Math.min(5, tasks.size());
                for (int i = 0; i < taskCount; i++) {
                    Task task = tasks.get(i);
                    HBox taskItem = createTaskItem(task);
                    tasksList.getChildren().add(taskItem);

                    // Add entrance animation with delay
                    addTaskItemAnimation(taskItem, i * 100);
                }

                if (tasks.size() > 5) {
                    Label moreLabel = new Label("+" + (tasks.size() - 5) + " more tasks");
                    moreLabel.setStyle("-fx-text-fill: #667eea; -fx-font-size: 12px; -fx-padding: 10; -fx-cursor: hand;");
                    moreLabel.setOnMouseClicked(e -> showTasks());
                    tasksList.getChildren().add(moreLabel);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating tasks list: " + e.getMessage());
        }
    }

    private void addTaskItemAnimation(Node taskItem, double delayMs) {
        try {
            taskItem.setOpacity(0);
            taskItem.setTranslateX(-20);

            PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(300), taskItem);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(300), taskItem);
                slide.setFromX(-20);
                slide.setToX(0);
                slide.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition animation = new ParallelTransition(fade, slide);
                animation.play();
            });
            delay.play();
        } catch (Exception e) {
            // Fallback: just show the item
            taskItem.setOpacity(1);
            taskItem.setTranslateX(0);
        }
    }

    private HBox createTaskItem(Task task) {
        try {
            HBox taskItem = new HBox(12);
            taskItem.getStyleClass().add("task-item");

            // Priority indicator
            String priorityClass = "priority-" + task.getPriority().name().toLowerCase();
            taskItem.getStyleClass().add(priorityClass);

            // Checkbox with improved styling
            CheckBox checkbox = new CheckBox();
            checkbox.setSelected(task.getStatus() == Task.Status.COMPLETED);
            checkbox.setOnAction(e -> {
                try {
                    task.setStatus(checkbox.isSelected() ? Task.Status.COMPLETED : Task.Status.PENDING);
                    taskService.updateTask(task);

                    // Show feedback
                    String message = checkbox.isSelected() ?
                            "Task completed! Well done! ✅" : "Task marked as pending";
                    NotificationManager.getInstance().showNotification(
                            "Task Updated", message, NotificationManager.NotificationType.SUCCESS
                    );

                    // Refresh data
                    loadDashboardDataSafely();
                } catch (Exception ex) {
                    ErrorHandler.handleError("Task Update", "Failed to update task", ex);
                }
            });

            // Task details with better typography
            VBox taskDetails = new VBox(3);

            Label titleLabel = new Label(task.getTitle());
            titleLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 14px;");

            if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
                Label descLabel = new Label(task.getDescription());
                descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
                descLabel.setWrapText(true);
                taskDetails.getChildren().add(descLabel);
            }

            taskDetails.getChildren().add(0, titleLabel);

            // Due date with smart formatting
            VBox dueDateBox = new VBox(2);
            dueDateBox.setMinWidth(80);

            if (task.getDueDate() != null) {
                Label dueDateLabel = new Label(formatDueDate(task.getDueDate()));
                dueDateLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 500;");

                // Color coding for due dates
                if (task.getDueDate().isBefore(LocalDate.now()) &&
                        task.getStatus() != Task.Status.COMPLETED) {
                    dueDateLabel.setStyle(dueDateLabel.getStyle() + " -fx-text-fill: #ef4444;");
                } else if (task.getDueDate().equals(LocalDate.now())) {
                    dueDateLabel.setStyle(dueDateLabel.getStyle() + " -fx-text-fill: #f59e0b;");
                } else {
                    dueDateLabel.setStyle(dueDateLabel.getStyle() + " -fx-text-fill: #6b7280;");
                }

                dueDateBox.getChildren().add(dueDateLabel);
            }

            taskItem.getChildren().addAll(checkbox, taskDetails, dueDateBox);
            HBox.setHgrow(taskDetails, Priority.ALWAYS);

            // Add hover effect
            taskItem.setOnMouseEntered(e -> {
                ScaleTransition scale = new ScaleTransition(Duration.millis(100), taskItem);
                scale.setToX(1.01);
                scale.setToY(1.01);
                scale.play();
            });

            taskItem.setOnMouseExited(e -> {
                ScaleTransition scale = new ScaleTransition(Duration.millis(100), taskItem);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.play();
            });

            return taskItem;
        } catch (Exception e) {
            System.err.println("Error creating task item: " + e.getMessage());
            // Return simple fallback
            HBox fallback = new HBox();
            fallback.getChildren().add(new Label(task.getTitle()));
            return fallback;
        }
    }

    private String formatDueDate(LocalDate dueDate) {
        LocalDate today = LocalDate.now();

        if (dueDate.equals(today)) {
            return "Today";
        } else if (dueDate.equals(today.plusDays(1))) {
            return "Tomorrow";
        } else if (dueDate.isBefore(today)) {
            long daysAgo = today.toEpochDay() - dueDate.toEpochDay();
            return daysAgo == 1 ? "Yesterday" : daysAgo + " days ago";
        } else {
            long daysUntil = dueDate.toEpochDay() - today.toEpochDay();
            return "In " + daysUntil + " days";
        }
    }

    private void updateStatistics(List<Task> tasks) {
        try {
            // Get completed tasks count
            long completedTasks = tasks.stream()
                    .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                    .count();

            if (tasksCompletedLabel != null) {
                if (completedTasks > 0) {
                    tasksCompletedLabel.setText(completedTasks + " completed");
                } else {
                    tasksCompletedLabel.setText("No completed tasks yet");
                }
            }

            // Focus time should be empty for new users
            if (focusTimeLabel != null) {
                focusTimeLabel.setText("No focus time recorded");
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    private void startPeriodicUpdates() {
        try {
            // Update dashboard data every 5 minutes
            Timeline updateTimeline = new Timeline(
                    new KeyFrame(Duration.minutes(5), e -> loadDashboardDataSafely())
            );
            updateTimeline.setCycleCount(Timeline.INDEFINITE);
            updateTimeline.play();

            System.out.println("✅ Periodic updates started");
        } catch (Exception e) {
            System.err.println("Error starting periodic updates: " + e.getMessage());
        }
    }

    private void setActiveButton(Button activeBtn) {
        try {
            if (activeBtn == null) return;

            // Remove active class from all buttons
            Button[] buttons = {dashboardBtn, tasksBtn, pomodoroBtn, moodBtn, notesBtn, goalsBtn, exportBtn};
            for (Button btn : buttons) {
                if (btn != null) {
                    btn.getStyleClass().remove("active");
                }
            }

            // Add active class to current button
            activeBtn.getStyleClass().add("active");
        } catch (Exception e) {
            System.err.println("Error setting active button: " + e.getMessage());
        }
    }

    private void showDashboard() {
        if (!currentView.equals("dashboard")) {
            try {
                setActiveButton(dashboardBtn);
                currentView = "dashboard";

                // Restore original dashboard content
                contentArea.getChildren().clear();
                if (dashboardContent != null) {
                    contentArea.getChildren().add(dashboardContent);
                }

                // Re-setup timer if needed
                setupPomodoroTimerSafely();
                loadDashboardDataSafely();
            } catch (Exception e) {
                ErrorHandler.handleError("Navigation", "Failed to show dashboard", e);
            }
        }
    }

    private void showTasks() {
        setActiveButton(tasksBtn);
        currentView = "tasks";
        loadView("/fxml/tasks.fxml");
    }

    private void showPomodoro() {
        setActiveButton(pomodoroBtn);
        currentView = "pomodoro";
        showDashboard(); // Pomodoro is part of dashboard
    }

    private void showMoodTracker() {
        setActiveButton(moodBtn);
        currentView = "mood";
        loadView("/fxml/mood-tracker.fxml");
    }

    private void showNotes() {
        setActiveButton(notesBtn);
        currentView = "notes";
        loadView("/fxml/notes.fxml");
    }

    private void showGoals() {
        setActiveButton(goalsBtn);
        currentView = "goals";
        loadView("/fxml/goals.fxml");
    }

    private void showExport() {
        setActiveButton(exportBtn);
        currentView = "export";
        loadView("/fxml/export.fxml");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node view = loader.load();

            // Add loading animation
            view.setOpacity(0);
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);

            // Ensure the loaded view fills the content area
            if (view instanceof Region) {
                Region region = (Region) view;
                region.setPrefWidth(Region.USE_COMPUTED_SIZE);
                region.setPrefHeight(Region.USE_COMPUTED_SIZE);
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }

            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), view);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

        } catch (Exception e) {
            ErrorHandler.handleError("Navigation", "Failed to load view: " + fxmlPath, e);

            // Fallback: show error message
            Label errorLabel = new Label("Failed to load " + currentView + " view");
            errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 16px; -fx-padding: 50;");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(errorLabel);
        }
    }

    private void handleLogout() {
        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Logout");
            alert.setHeaderText("Are you sure you want to logout?");
            alert.setContentText("Any unsaved changes will be lost.");

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        // Show logout animation
                        showLogoutAnimation();

                        UserSession.getInstance().logout();
                        navigateToLogin();
                    } catch (Exception e) {
                        ErrorHandler.handleError("Logout", "Failed to logout", e);
                    }
                }
            });
        } catch (Exception e) {
            ErrorHandler.handleError("Logout", "Failed to show logout dialog", e);
        }
    }

    private void showLogoutAnimation() {
        try {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), dashboardContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.3);
            fadeOut.play();
        } catch (Exception e) {
            System.err.println("Error showing logout animation: " + e.getMessage());
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));

            Stage stage = (Stage) dashboardContainer.getScene().getWindow();

            // Get screen dimensions
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

            // Create login scene
            Scene scene = new Scene(loader.load());

            ThemeManager.getInstance().applyTheme(scene, ThemeManager.getInstance().getCurrentTheme());

            stage.setScene(scene);
            stage.setTitle("FocusBuddy - Login");

            // Reset window to normal size for login
            stage.setMaximized(false);
            stage.setResizable(true);

            double loginWidth = Math.min(1200, screenBounds.getWidth() * 0.8);
            double loginHeight = Math.min(800, screenBounds.getHeight() * 0.85);

            stage.setWidth(loginWidth);
            stage.setHeight(loginHeight);

            // Center window
            stage.setX((screenBounds.getWidth() - loginWidth) / 2);
            stage.setY((screenBounds.getHeight() - loginHeight) / 2);

        } catch (Exception e) {
            ErrorHandler.handleError("Navigation", "Failed to navigate to login", e);
        }
    }
}