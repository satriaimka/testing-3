package com.focusbuddy.controllers;

import com.focusbuddy.models.Task;
import com.focusbuddy.services.ActivityService;
import com.focusbuddy.models.ActivityItem;
import com.focusbuddy.models.User;
import com.focusbuddy.services.PomodoroTimer;
import com.focusbuddy.services.TaskService;
import com.focusbuddy.services.MoodService;
import com.focusbuddy.services.GoalsService;
import com.focusbuddy.utils.ThemeManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.ErrorHandler;
import com.focusbuddy.models.RegularUser;
import com.focusbuddy.services.UserService;
import com.focusbuddy.controllers.ProfileDialogController;
import javafx.stage.Modality;
import javafx.animation.ScaleTransition;
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

    // ✅ NEW: Dashboard action buttons
    @FXML private Button viewAllTasksBtn;
    @FXML private Button addNewTaskBtn;
    @FXML private Button viewAllActivitiesBtn;

    // ✅ NEW: Quick action buttons
    @FXML private Button quickNewNoteBtn;
    @FXML private Button quickSetGoalBtn;
    @FXML private Button quickLogMoodBtn;
    @FXML private Button quickExportBtn;

    // Dashboard content elements
    @FXML private Label timerDisplay;
    @FXML private Button startTimerBtn;
    @FXML private Button pauseTimerBtn;
    @FXML private Button resetTimerBtn;
    @FXML private ProgressBar timerProgress;
    @FXML private VBox tasksList;

    // Dashboard statistics labels - ALL REAL DATA FROM DATABASE
    @FXML private Label tasksCompletedLabel;
    @FXML private Label focusTimeLabel;
    @FXML private Label goalsProgressLabel;
    @FXML private Label moodAverageLabel;
    @FXML private Label streakLabel;
    @FXML private Label efficiencyLabel;
    @FXML private Label todayProgressLabel;
    @FXML private Label recentActivityLabel;

    // Progress bars for visual indicators
    @FXML private ProgressBar tasksProgress;
    @FXML private ProgressBar focusProgress;
    @FXML private ProgressBar goalsProgress;
    @FXML private ProgressBar moodProgress;
    @FXML private ProgressBar todayTasksProgress;
    @FXML private ProgressBar todayFocusProgress;
    @FXML private VBox recentActivityContainer;
    @FXML private VBox userProfileSection; // Reference ke bagian profile di sidebar


    private PomodoroTimer pomodoroTimer;
    private TaskService taskService;
    private MoodService moodService;
    private GoalsService goalsService;
    private String currentView = "dashboard";
    private Node dashboardContent;
    private ActivityService activityService;
    private UserService userService; // Service untuk operasi user

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

            // ✅ NEW: Set up dashboard action buttons
            setupDashboardActionButtons();

            // ✅ NEW: Set up quick action buttons
            setupQuickActionButtons();

            setupUserProfileSection();

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
                    // ✅ LOAD REAL DATA FROM DATABASE
                    loadRealDashboardData();
                    startPeriodicUpdates();
                } catch (Exception e) {
                    ErrorHandler.handleError("Dashboard Initialization",
                            "Failed to complete dashboard setup", e);
                    clearDashboardStats(); // Ensure clean state on error
                }
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Dashboard Initialization Error",
                    "Failed to initialize dashboard", e);
        }
    }

    // ✅ NEW: Setup dashboard action buttons
    private void setupDashboardActionButtons() {
        try {
            if (viewAllTasksBtn != null) {
                viewAllTasksBtn.setOnAction(e -> handleViewAllTasks());
                addButtonHoverEffect(viewAllTasksBtn);
            }

            if (addNewTaskBtn != null) {
                addNewTaskBtn.setOnAction(e -> handleAddNewTask());
                addButtonHoverEffect(addNewTaskBtn);
            }

            if (viewAllActivitiesBtn != null) {
                viewAllActivitiesBtn.setOnAction(e -> handleViewAllActivities());
                addButtonHoverEffect(viewAllActivitiesBtn);
            }

            System.out.println("✅ Dashboard action buttons setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Dashboard Actions Setup", "Failed to setup dashboard action buttons", e);
        }
    }

    // ✅ NEW: Setup quick action buttons
    private void setupQuickActionButtons() {
        try {
            if (quickNewNoteBtn != null) {
                quickNewNoteBtn.setOnAction(e -> handleQuickNewNote());
                addButtonHoverEffect(quickNewNoteBtn);
            }

            if (quickSetGoalBtn != null) {
                quickSetGoalBtn.setOnAction(e -> handleQuickSetGoal());
                addButtonHoverEffect(quickSetGoalBtn);
            }

            if (quickLogMoodBtn != null) {
                quickLogMoodBtn.setOnAction(e -> handleQuickLogMood());
                addButtonHoverEffect(quickLogMoodBtn);
            }

            if (quickExportBtn != null) {
                quickExportBtn.setOnAction(e -> handleQuickExport());
                addButtonHoverEffect(quickExportBtn);
            }

            System.out.println("✅ Quick action buttons setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Quick Actions Setup", "Failed to setup quick action buttons", e);
        }
    }

    // ✅ NEW: Handle View All Tasks
    private void handleViewAllTasks() {
        try {
            System.out.println("🔍 Navigate to All Tasks view");

            // Add button click animation
            addButtonClickAnimation(viewAllTasksBtn);

            // Navigate to tasks view
            showTasks();

            // Show feedback
            NotificationManager.getInstance().showNotification(
                    "Navigation",
                    "Showing all your tasks 📋",
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            ErrorHandler.handleError("View All Tasks", "Failed to navigate to tasks view", e);
        }
    }

    // ✅ NEW: Handle Add New Task
    private void handleAddNewTask() {
        try {
            System.out.println("➕ Navigate to create new task");

            // Add button click animation
            addButtonClickAnimation(addNewTaskBtn);

            // Navigate to tasks view (which allows creating new tasks)
            showTasks();

            // Show feedback with tips
            NotificationManager.getInstance().showNotification(
                    "Create Task",
                    "Ready to add a new task! 📝\nTip: Be specific and set a due date",
                    NotificationManager.NotificationType.SUCCESS
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Add New Task", "Failed to navigate to task creation", e);
        }
    }

    // ✅ NEW: Handle View All Activities
    private void handleViewAllActivities() {
        try {
            System.out.println("📊 Show expanded activity view");

            // Add button click animation
            addButtonClickAnimation(viewAllActivitiesBtn);

            // For now, show a comprehensive activity summary
            showExpandedActivitySummary();

        } catch (Exception e) {
            ErrorHandler.handleError("View All Activities", "Failed to show activity summary", e);
        }
    }

    // ✅ NEW: Handle Quick New Note
    private void handleQuickNewNote() {
        try {
            System.out.println("📝 Quick action: Create new note");

            // Add button click animation
            addButtonClickAnimation(quickNewNoteBtn);

            // Navigate to notes view
            showNotes();

            // Show feedback
            NotificationManager.getInstance().showNotification(
                    "Quick Note",
                    "Ready to capture your thoughts! ✨",
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Quick New Note", "Failed to navigate to notes", e);
        }
    }

    // ✅ NEW: Handle Quick Set Goal
    private void handleQuickSetGoal() {
        try {
            System.out.println("🎯 Quick action: Set new goal");

            // Add button click animation
            addButtonClickAnimation(quickSetGoalBtn);

            // Navigate to goals view
            showGoals();

            // Show feedback with motivation
            NotificationManager.getInstance().showNotification(
                    "Set Goal",
                    "Time to set an ambitious goal! 🚀\nRemember: dreams become goals with deadlines",
                    NotificationManager.NotificationType.SUCCESS
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Quick Set Goal", "Failed to navigate to goals", e);
        }
    }

    // ✅ NEW: Handle Quick Log Mood
    private void handleQuickLogMood() {
        try {
            System.out.println("😊 Quick action: Log mood");

            // Add button click animation
            addButtonClickAnimation(quickLogMoodBtn);

            // Navigate to mood tracker
            showMoodTracker();

            // Show feedback
            NotificationManager.getInstance().showNotification(
                    "Mood Check",
                    "How are you feeling today? 🌈\nTracking mood helps build self-awareness",
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Quick Log Mood", "Failed to navigate to mood tracker", e);
        }
    }

    // ✅ NEW: Handle Quick Export
    private void handleQuickExport() {
        try {
            System.out.println("💾 Quick action: Export data");

            // Add button click animation
            addButtonClickAnimation(quickExportBtn);

            // Navigate to export view
            showExport();

            // Show feedback
            NotificationManager.getInstance().showNotification(
                    "Export Data",
                    "Backup and export your productivity data 📦",
                    NotificationManager.NotificationType.INFO
            );

        } catch (Exception e) {
            ErrorHandler.handleError("Quick Export", "Failed to navigate to export", e);
        }
    }

    // ✅ NEW: Show expanded activity summary
    private void showExpandedActivitySummary() {
        try {
            int userId = UserSession.getInstance().getCurrentUser().getId();

            // Get comprehensive activity data
            CompletableFuture.supplyAsync(() -> {
                try {
                    return activityService.getRecentActivities(userId, 20); // Get more activities
                } catch (Exception e) {
                    System.err.println("Error loading extended activities: " + e.getMessage());
                    return List.<ActivityItem>of();
                }
            }).thenAccept(activities -> {
                Platform.runLater(() -> {
                    try {
                        showActivitySummaryDialog(activities);
                    } catch (Exception e) {
                        ErrorHandler.handleError("Activity Summary", "Failed to display activity summary", e);
                    }
                });
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Expanded Activity", "Failed to load expanded activity view", e);
        }
    }

    // ✅ NEW: Show activity summary dialog
    private void showActivitySummaryDialog(List<ActivityItem> activities) {
        try {
            Alert dialog = new Alert(Alert.AlertType.INFORMATION);
            dialog.setTitle("Recent Activity Summary");
            dialog.setHeaderText("Your productivity journey 📈");

            // Create scrollable content
            VBox content = new VBox(10);
            content.setPrefWidth(500);
            content.setPrefHeight(400);

            if (activities.isEmpty()) {
                Label emptyLabel = new Label("No recent activities found.\nStart being productive to see your journey here! 🌟");
                emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-alignment: center;");
                content.getChildren().add(emptyLabel);
            } else {
                // Activity summary
                Label summaryLabel = new Label("📊 Summary: " + activities.size() + " recent activities");
                summaryLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                content.getChildren().add(summaryLabel);

                // Add separator
                content.getChildren().add(new Separator());

                // Activity list
                ScrollPane scrollPane = new ScrollPane();
                VBox activitiesBox = new VBox(8);

                for (ActivityItem activity : activities) {
                    HBox activityRow = new HBox(10);
                    activityRow.setStyle("-fx-padding: 8; -fx-background-color: #f8f9fa; -fx-background-radius: 4;");

                    Label iconLabel = new Label(activity.getIcon());
                    iconLabel.setStyle("-fx-font-size: 14px;");

                    VBox activityInfo = new VBox(2);
                    Label titleLabel = new Label(activity.getTitle());
                    titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

                    Label timeLabel = new Label(activity.getTimeAgo());
                    timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

                    activityInfo.getChildren().addAll(titleLabel, timeLabel);
                    activityRow.getChildren().addAll(iconLabel, activityInfo);

                    activitiesBox.getChildren().add(activityRow);
                }

                scrollPane.setContent(activitiesBox);
                scrollPane.setFitToWidth(true);
                scrollPane.setPrefHeight(300);
                content.getChildren().add(scrollPane);
            }

            // Set dialog content
            dialog.getDialogPane().setContent(content);

            // Apply current theme
            try {
                if (ThemeManager.getInstance().getCurrentTheme() != null) {
                    String cssPath = "/css/" + ThemeManager.getInstance().getCurrentTheme().getCssFile();
                    dialog.getDialogPane().getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                }
            } catch (Exception e) {
                // Ignore styling errors
            }

            dialog.showAndWait();

        } catch (Exception e) {
            ErrorHandler.handleError("Activity Dialog", "Failed to show activity summary dialog", e);
        }
    }

    // ✅ NEW: Add button click animation
    private void addButtonClickAnimation(Button button) {
        try {
            if (button != null) {
                // Quick scale animation to show button was clicked
                ScaleTransition clickAnimation = new ScaleTransition(Duration.millis(100), button);
                clickAnimation.setFromX(1.0);
                clickAnimation.setFromY(1.0);
                clickAnimation.setToX(0.95);
                clickAnimation.setToY(0.95);
                clickAnimation.setCycleCount(2);
                clickAnimation.setAutoReverse(true);
                clickAnimation.play();
            }
        } catch (Exception e) {
            // Ignore animation errors
        }
    }

    private void initializeServices() {
        try {
            taskService = new TaskService();
            moodService = new MoodService();
            goalsService = new GoalsService();
            pomodoroTimer = new PomodoroTimer();
            activityService = new ActivityService();
            userService = new UserService(); // ✅ TAMBAH BARIS INI
            System.out.println("✅ Services initialized successfully");
        } catch (Exception e) {
            ErrorHandler.handleError("Service Initialization",
                    "Failed to initialize dashboard services", e);
            // Fallback: create empty instances
            if (taskService == null) taskService = new TaskService();
            if (moodService == null) moodService = new MoodService();
            if (goalsService == null) goalsService = new GoalsService();
            if (pomodoroTimer == null) pomodoroTimer = new PomodoroTimer();
            if (activityService == null) activityService = new ActivityService();
            if (userService == null) userService = new UserService(); // ✅ TAMBAH BARIS INI
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
            System.out.println("🍅 Setting up Pomodoro timer...");

            // Set up timer display update with enhanced error handling
            pomodoroTimer.setOnTimeUpdate((minutes, seconds) -> {
                Platform.runLater(() -> {
                    try {
                        if (timerDisplay != null) {
                            String timeText = String.format("%02d:%02d", minutes, seconds);
                            timerDisplay.setText(timeText);

                            // Debug output
                            if (pomodoroTimer.isRunning()) {
                                System.out.println("⏰ Timer display updated: " + timeText);
                            }
                        }

                        if (timerProgress != null) {
                            double progress = pomodoroTimer.getProgress();
                            timerProgress.setProgress(progress);

                            // Debug output
                            if (pomodoroTimer.isRunning()) {
                                System.out.println("📊 Progress updated: " + String.format("%.1f%%", progress * 100));
                            }

                            // Add visual feedback when timer is running
                            if (pomodoroTimer.isRunning() && progress > 0) {
                                addTimerPulseAnimation();
                            }
                        }

                        // Update timer button states
                        updateTimerButtonStates();

                    } catch (Exception e) {
                        System.err.println("Error updating timer display: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            });

            // Set up timer completion with database integration
            pomodoroTimer.setOnTimerComplete(() -> {
                Platform.runLater(() -> {
                    try {
                        String sessionType = pomodoroTimer.isFocusSession() ? "Break" : "Focus"; // Note: inverted because it switches after completion
                        String completedType = pomodoroTimer.isFocusSession() ? "Focus" : "Break";

                        NotificationManager.getInstance().showNotification(
                                completedType + " Session Complete! 🎉",
                                completedType.equals("Focus") ?
                                        "Great job! Time for a well-deserved break. 😌" :
                                        "Break's over! Ready for another focus session? 💪",
                                NotificationManager.NotificationType.SUCCESS
                        );

                        // Add completion animation
                        addTimerCompletionAnimation();

                        // Update timer button states
                        updateTimerButtonStates();

                        // Refresh dashboard data to show updated focus time
                        Platform.runLater(() -> {
                            try {
                                loadRealDashboardData();
                            } catch (Exception e) {
                                System.err.println("Error refreshing dashboard after timer completion: " + e.getMessage());
                            }
                        });

                        System.out.println("✅ " + completedType + " session completed successfully!");

                    } catch (Exception e) {
                        System.err.println("Error handling timer completion: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            });

            // Set up timer controls with enhanced functionality
            setupTimerControls();

            // Initialize display with proper formatting
            if (timerDisplay != null) {
                timerDisplay.setText("25:00");
            }
            if (timerProgress != null) {
                timerProgress.setProgress(0.0);
            }

            // Update button states
            updateTimerButtonStates();

            System.out.println("✅ Pomodoro timer setup completed successfully");

        } catch (Exception e) {
            ErrorHandler.handleError("Timer Setup", "Failed to setup Pomodoro timer", e);

            // Fallback: Create new timer instance
            try {
                pomodoroTimer = new PomodoroTimer();
                System.out.println("🔄 Created new timer instance as fallback");
            } catch (Exception ex) {
                System.err.println("Failed to create fallback timer: " + ex.getMessage());
            }
        }
    }

    private void setupTimerControls() {
        try {
            // Start Timer Button
            if (startTimerBtn != null) {
                startTimerBtn.setOnAction(e -> {
                    try {
                        if (!pomodoroTimer.isRunning()) {
                            pomodoroTimer.start();

                            String sessionType = pomodoroTimer.isFocusSession() ? "Focus" : "Break";
                            showTimerFeedback("✨ " + sessionType + " session started! Stay focused. 🎯");

                            // Update button states
                            updateTimerButtonStates();

                            System.out.println("▶️ Timer started: " + sessionType + " session");
                        }
                    } catch (Exception ex) {
                        ErrorHandler.handleError("Timer Start", "Failed to start timer", ex);
                    }
                });
                addButtonHoverEffect(startTimerBtn);
            }

            // Pause Timer Button
            if (pauseTimerBtn != null) {
                pauseTimerBtn.setOnAction(e -> {
                    try {
                        if (pomodoroTimer.isRunning()) {
                            pomodoroTimer.pause();
                            showTimerFeedback("⏸️ Timer paused. Take a moment to breathe. 😌");

                            // Update button states
                            updateTimerButtonStates();

                            System.out.println("⏸️ Timer paused");
                        }
                    } catch (Exception ex) {
                        ErrorHandler.handleError("Timer Pause", "Failed to pause timer", ex);
                    }
                });
                addButtonHoverEffect(pauseTimerBtn);
            }

            // Reset Timer Button
            if (resetTimerBtn != null) {
                resetTimerBtn.setOnAction(e -> {
                    try {
                        pomodoroTimer.reset();

                        if (timerDisplay != null) {
                            timerDisplay.setText("25:00");
                        }
                        if (timerProgress != null) {
                            timerProgress.setProgress(0.0);
                        }

                        showTimerFeedback("🔄 Timer reset. Ready for a fresh start! ✨");

                        // Update button states
                        updateTimerButtonStates();

                        System.out.println("🔄 Timer reset successfully");

                    } catch (Exception ex) {
                        ErrorHandler.handleError("Timer Reset", "Failed to reset timer", ex);
                    }
                });
                addButtonHoverEffect(resetTimerBtn);
            }

            System.out.println("✅ Timer controls setup completed");

        } catch (Exception e) {
            System.err.println("Error setting up timer controls: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateTimerButtonStates() {
        try {
            if (pomodoroTimer == null) return;

            boolean isRunning = pomodoroTimer.isRunning();

            // Update button availability
            if (startTimerBtn != null) {
                startTimerBtn.setDisable(isRunning);
                if (isRunning) {
                    startTimerBtn.getStyleClass().add("disabled");
                } else {
                    startTimerBtn.getStyleClass().remove("disabled");
                }
            }

            if (pauseTimerBtn != null) {
                pauseTimerBtn.setDisable(!isRunning);
                if (!isRunning) {
                    pauseTimerBtn.getStyleClass().add("disabled");
                } else {
                    pauseTimerBtn.getStyleClass().remove("disabled");
                }
            }

            if (resetTimerBtn != null) {
                resetTimerBtn.setDisable(false); // Reset always available
            }

            // Update timer type indicator
            if (timerDisplay != null) {
                String sessionType = pomodoroTimer.isFocusSession() ? "Focus" : "Break";
                // Could add session type to UI if needed
            }

        } catch (Exception e) {
            System.err.println("Error updating timer button states: " + e.getMessage());
        }
    }

    private void addTimerPulseAnimation() {
        if (timerDisplay == null || !pomodoroTimer.isRunning()) return;

        try {
            // Only pulse every 5 seconds to avoid too much animation
            int currentSeconds = pomodoroTimer.getCurrentSeconds();
            if (currentSeconds % 5 != 0) return;

            ScaleTransition pulse = new ScaleTransition(Duration.seconds(0.8), timerDisplay);
            pulse.setFromX(1.0);
            pulse.setFromY(1.0);
            pulse.setToX(1.02);
            pulse.setToY(1.02);
            pulse.setCycleCount(2);
            pulse.setAutoReverse(true);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.play();

            // Add progress bar glow effect
            if (timerProgress != null) {
                FadeTransition glow = new FadeTransition(Duration.seconds(0.8), timerProgress);
                glow.setFromValue(1.0);
                glow.setToValue(0.7);
                glow.setCycleCount(2);
                glow.setAutoReverse(true);
                glow.play();
            }

        } catch (Exception e) {
            System.err.println("Error adding timer pulse animation: " + e.getMessage());
        }
    }


    private void addTimerCompletionAnimation() {
        if (timerDisplay == null) return;

        try {
            // Flash animation for completion
            FadeTransition flash = new FadeTransition(Duration.millis(150), timerDisplay);
            flash.setFromValue(1.0);
            flash.setToValue(0.2);
            flash.setCycleCount(8);
            flash.setAutoReverse(true);
            flash.setInterpolator(Interpolator.EASE_BOTH);

            // Scale animation
            ScaleTransition scale = new ScaleTransition(Duration.millis(300), timerDisplay);
            scale.setFromX(1.0);
            scale.setFromY(1.0);
            scale.setToX(1.1);
            scale.setToY(1.1);
            scale.setCycleCount(2);
            scale.setAutoReverse(true);
            scale.setInterpolator(Interpolator.EASE_OUT);

            // Progress bar completion effect
            if (timerProgress != null) {
                Timeline progressComplete = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(timerProgress.progressProperty(), timerProgress.getProgress())),
                        new KeyFrame(Duration.millis(500), new KeyValue(timerProgress.progressProperty(), 1.0)),
                        new KeyFrame(Duration.millis(1000), new KeyValue(timerProgress.progressProperty(), 0.0))
                );
                progressComplete.play();
            }

            // Play animations
            ParallelTransition completion = new ParallelTransition(flash, scale);
            completion.play();

        } catch (Exception e) {
            System.err.println("Error adding timer completion animation: " + e.getMessage());
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

    // ✅ LOAD REAL DATA FROM DATABASE
    private void loadRealDashboardData() {
        if (taskService == null) {
            System.out.println("⚠️ Task service not available, skipping data load");
            clearDashboardStats();
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
            }).thenAccept(userTasks -> {
                Platform.runLater(() -> {
                    try {
                        // Update tasks list
                        updateTasksList(userTasks);

                        // Update all statistics with REAL data
                        updateRealStatistics(userId, userTasks);

                        // Load recent activity
                        updateRecentActivity(userId);

                        System.out.println("✅ Real dashboard data loaded successfully");
                    } catch (Exception e) {
                        System.err.println("Error updating dashboard data: " + e.getMessage());
                        clearDashboardStats(); // Fallback to clean state
                    }
                });
            });
        } catch (Exception e) {
            ErrorHandler.handleError("Dashboard Data", "Failed to load dashboard data", e);
            clearDashboardStats();
        }

        updateUserProfileDisplay();
    }

    // ✅ UPDATE STATISTICS WITH REAL DATA FROM DATABASE
    private void updateRealStatistics(int userId, List<Task> userTasks) {
        try {
            // Tasks Statistics (existing code...)
            long completedToday = userTasks.stream()
                    .filter(task -> task.getStatus() == Task.Status.COMPLETED &&
                            task.getCreatedAt() != null &&
                            task.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                    .count();

            long totalTasks = userTasks.size();
            double taskProgress = totalTasks > 0 ? (double) completedToday / totalTasks : 0.0;

            if (tasksCompletedLabel != null) {
                if (completedToday > 0) {
                    tasksCompletedLabel.setText(completedToday + " completed");
                } else {
                    tasksCompletedLabel.setText("No tasks completed");
                }
            }
            if (tasksProgress != null) {
                tasksProgress.setProgress(taskProgress);
            }

            // ✅ ENHANCED FOCUS TIME - Load from database
            loadFocusTimeFromDatabase(userId);

            // Goals Progress - Load from database (existing code...)
            loadGoalsProgress(userId);

            // Mood Average - Load from database (existing code...)
            loadMoodAverage(userId);

            // Productivity Insights (existing code...)
            updateProductivityInsights(userId, userTasks);

            // Today's Progress (existing code...)
            updateTodayProgress(userTasks);

        } catch (Exception e) {
            System.err.println("Error updating real statistics: " + e.getMessage());
            clearDashboardStats();
        }
    }

    private void loadFocusTimeFromDatabase(int userId) {
        try {
            // Load focus time asynchronously
            CompletableFuture.supplyAsync(() -> {
                try {
                    int todayMinutes = PomodoroTimer.getTodayFocusMinutes(userId);
                    int weekMinutes = PomodoroTimer.getWeekFocusMinutes(userId);
                    return new int[]{todayMinutes, weekMinutes};
                } catch (Exception e) {
                    System.err.println("Error loading focus time: " + e.getMessage());
                    return new int[]{0, 0};
                }
            }).thenAccept(focusData -> {
                Platform.runLater(() -> {
                    try {
                        int todayMinutes = focusData[0];
                        int weekMinutes = focusData[1];

                        if (focusTimeLabel != null) {
                            if (todayMinutes > 0) {
                                String focusText = formatFocusTime(todayMinutes);
                                focusTimeLabel.setText(focusText + " today");
                            } else {
                                focusTimeLabel.setText("No focus time today");
                            }
                        }

                        if (focusProgress != null) {
                            // Progress based on daily goal (e.g., 2 hours = 120 minutes)
                            int dailyGoalMinutes = 120; // 2 hours goal
                            double progress = Math.min(1.0, (double) todayMinutes / dailyGoalMinutes);
                            focusProgress.setProgress(progress);
                        }

                        // Update today's focus progress
                        if (todayFocusProgress != null) {
                            int dailyGoalMinutes = 120;
                            double progress = Math.min(1.0, (double) todayMinutes / dailyGoalMinutes);
                            todayFocusProgress.setProgress(progress);
                        }

                        System.out.println("✅ Focus time loaded: " + todayMinutes + " min today, " + weekMinutes + " min this week");

                    } catch (Exception e) {
                        System.err.println("Error updating focus time UI: " + e.getMessage());
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("Error loading focus time from database: " + e.getMessage());
        }
    }

    private String formatFocusTime(int minutes) {
        if (minutes < 60) {
            return minutes + "m";
        } else {
            int hours = minutes / 60;
            int remainingMinutes = minutes % 60;
            if (remainingMinutes == 0) {
                return hours + "h";
            } else {
                return hours + "h " + remainingMinutes + "m";
            }
        }
    }

    private void showTimerFeedback(String message) {
        try {
            NotificationManager.getInstance().showNotification(
                    "🍅 Pomodoro Timer", message, NotificationManager.NotificationType.INFO
            );
        } catch (Exception e) {
            System.err.println("Error showing timer feedback: " + e.getMessage());
        }
    }

    private void loadGoalsProgress(int userId) {
        try {
            if (goalsService != null) {
                CompletableFuture.supplyAsync(() -> {
                    try {
                        int totalGoals = goalsService.getTotalGoalsCount(userId);
                        int completedGoals = goalsService.getCompletedGoalsCount(userId);
                        return new int[]{totalGoals, completedGoals};
                    } catch (Exception e) {
                        return new int[]{0, 0};
                    }
                }).thenAccept(goalsData -> {
                    Platform.runLater(() -> {
                        int totalGoals = goalsData[0];
                        int completedGoals = goalsData[1];

                        if (goalsProgressLabel != null) {
                            if (totalGoals > 0) {
                                goalsProgressLabel.setText(completedGoals + "/" + totalGoals);
                            } else {
                                goalsProgressLabel.setText("No goals set");
                            }
                        }

                        if (goalsProgress != null) {
                            double progress = totalGoals > 0 ? (double) completedGoals / totalGoals : 0.0;
                            goalsProgress.setProgress(progress);
                        }
                    });
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading goals progress: " + e.getMessage());
        }
    }

    private void loadMoodAverage(int userId) {
        try {
            if (moodService != null) {
                CompletableFuture.supplyAsync(() -> {
                    try {
                        var recentMoods = moodService.getRecentMoodEntries(userId, 7);
                        if (!recentMoods.isEmpty()) {
                            double average = recentMoods.stream()
                                    .mapToInt(mood -> mood.getMoodLevel())
                                    .average()
                                    .orElse(0.0);
                            return average;
                        }
                        return 0.0;
                    } catch (Exception e) {
                        return 0.0;
                    }
                }).thenAccept(average -> {
                    Platform.runLater(() -> {
                        if (moodAverageLabel != null) {
                            if (average > 0) {
                                moodAverageLabel.setText(String.format("%.1f/5", average));
                            } else {
                                moodAverageLabel.setText("No mood entries");
                            }
                        }

                        if (moodProgress != null) {
                            moodProgress.setProgress(average / 5.0);
                        }
                    });
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading mood average: " + e.getMessage());
        }
    }

    private void updateProductivityInsights(int userId, List<Task> userTasks) {
        try {
            // Streak calculation (simplified)
            long streak = userTasks.stream()
                    .filter(task -> task.getStatus() == Task.Status.COMPLETED &&
                            task.getCreatedAt() != null &&
                            task.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                    .count();

            if (streakLabel != null) {
                if (streak > 0) {
                    streakLabel.setText(streak + " today");
                } else {
                    streakLabel.setText("No active streak");
                }
            }

            // Efficiency (completion rate)
            long totalTasks = userTasks.size();
            long completedTasks = userTasks.stream()
                    .filter(task -> task.getStatus() == Task.Status.COMPLETED)
                    .count();

            if (efficiencyLabel != null) {
                if (totalTasks > 0) {
                    double efficiency = (double) completedTasks / totalTasks * 100;
                    efficiencyLabel.setText(String.format("%.0f%%", efficiency));
                } else {
                    efficiencyLabel.setText("--");
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating productivity insights: " + e.getMessage());
        }
    }

    private void updateTodayProgress(List<Task> userTasks) {
        try {
            // Today's tasks progress
            long todayTasks = userTasks.stream()
                    .filter(task -> task.getCreatedAt() != null &&
                            task.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                    .count();

            long todayCompleted = userTasks.stream()
                    .filter(task -> task.getStatus() == Task.Status.COMPLETED &&
                            task.getCreatedAt() != null &&
                            task.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                    .count();

            if (todayProgressLabel != null) {
                if (todayTasks > 0) {
                    todayProgressLabel.setText(todayCompleted + "/" + todayTasks);
                } else {
                    todayProgressLabel.setText("No progress yet");
                }
            }

            if (todayTasksProgress != null) {
                double progress = todayTasks > 0 ? (double) todayCompleted / todayTasks : 0.0;
                todayTasksProgress.setProgress(progress);
            }

            // Today's focus progress (placeholder)
            if (todayFocusProgress != null) {
                todayFocusProgress.setProgress(0.0);
            }

        } catch (Exception e) {
            System.err.println("Error updating today's progress: " + e.getMessage());
        }
    }

    private void updateRecentActivity(int userId) {
        try {
            if (activityService == null) {
                showEmptyActivity();
                return;
            }

            // Load recent activities asynchronously
            CompletableFuture.supplyAsync(() -> {
                try {
                    return activityService.getRecentActivities(userId, 8); // Get max 8 activities
                } catch (Exception e) {
                    System.err.println("Error loading recent activities: " + e.getMessage());
                    return List.<ActivityItem>of();
                }
            }).thenAccept(activities -> {
                Platform.runLater(() -> {
                    try {
                        displayRecentActivities(activities);
                    } catch (Exception e) {
                        System.err.println("Error displaying recent activities: " + e.getMessage());
                        showEmptyActivity();
                    }
                });
            });

        } catch (Exception e) {
            System.err.println("Error updating recent activity: " + e.getMessage());
            showEmptyActivity();
        }
    }

    private void showEmptyActivity() {
        try {
            if (recentActivityContainer != null) {
                recentActivityContainer.getChildren().clear();

                VBox emptyState = new VBox(8);
                emptyState.setAlignment(javafx.geometry.Pos.CENTER);
                emptyState.setStyle("-fx-padding: 20;");

                Label emptyIcon = new Label("🌟");
                emptyIcon.setStyle("-fx-font-size: 24px;");

                Label emptyMessage = new Label("No recent activity yet");
                emptyMessage.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #6b7280;");

                Label emptySubtext = new Label("Start creating tasks, notes, or logging mood!");
                emptySubtext.setStyle("-fx-font-size: 10px; -fx-text-fill: #9ca3af;");
                emptySubtext.setWrapText(true);
                emptySubtext.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

                emptyState.getChildren().addAll(emptyIcon, emptyMessage, emptySubtext);
                recentActivityContainer.getChildren().add(emptyState);
            }
        } catch (Exception e) {
            System.err.println("Error showing empty activity state: " + e.getMessage());
        }
    }

    private void displayRecentActivities(List<ActivityItem> activities) {
        try {
            if (recentActivityContainer != null) {
                recentActivityContainer.getChildren().clear();

                if (activities.isEmpty()) {
                    showEmptyActivity();
                    return;
                }

                // Display activities
                for (int i = 0; i < Math.min(activities.size(), 6); i++) {
                    ActivityItem activity = activities.get(i);
                    VBox activityItem = createActivityItemUI(activity);
                    recentActivityContainer.getChildren().add(activityItem);

                    // Add entrance animation with delay
                    addActivityItemAnimation(activityItem, i * 100);
                }

                // Add "View All" link if there are more activities
                if (activities.size() > 6) {
                    Label viewAllLabel = new Label("View all activities (" + activities.size() + ")");
                    viewAllLabel.setStyle("-fx-text-fill: #667eea; -fx-font-size: 10px; -fx-padding: 8 0 0 0; -fx-cursor: hand; -fx-underline: true;");
                    viewAllLabel.setOnMouseClicked(e -> {
                        // ✅ UPDATED: Navigate to actual view all activities
                        handleViewAllActivities();
                    });
                    recentActivityContainer.getChildren().add(viewAllLabel);
                }
            }
        } catch (Exception e) {
            System.err.println("Error displaying recent activities: " + e.getMessage());
            showEmptyActivity();
        }
    }

    private VBox createActivityItemUI(ActivityItem activity) {
        try {
            VBox itemContainer = new VBox(3);
            itemContainer.setStyle("-fx-padding: 8; -fx-background-color: transparent;");
            itemContainer.getStyleClass().add("activity-item");

            // Top row: Icon + Title
            HBox topRow = new HBox(8);
            topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label iconLabel = new Label(activity.getIcon());
            iconLabel.setStyle("-fx-font-size: 12px;");
            iconLabel.setMinWidth(16);

            Label titleLabel = new Label(activity.getTitle());
            titleLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #374151;");
            titleLabel.setWrapText(true);
            titleLabel.setMaxWidth(180);
            HBox.setHgrow(titleLabel, javafx.scene.layout.Priority.ALWAYS);

            topRow.getChildren().addAll(iconLabel, titleLabel);

            // Bottom row: Description + Time
            HBox bottomRow = new HBox(8);
            bottomRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            // Spacer for alignment with icon
            Region spacer = new Region();
            spacer.setMinWidth(16);

            VBox textContainer = new VBox(1);
            HBox.setHgrow(textContainer, javafx.scene.layout.Priority.ALWAYS);

            if (activity.getDescription() != null && !activity.getDescription().trim().isEmpty()) {
                Label descLabel = new Label(activity.getDescription());
                descLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #6b7280;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(160);
                textContainer.getChildren().add(descLabel);
            }

            Label timeLabel = new Label(activity.getTimeAgo());
            timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #9ca3af; -fx-font-style: italic;");
            textContainer.getChildren().add(timeLabel);

            bottomRow.getChildren().addAll(spacer, textContainer);

            itemContainer.getChildren().addAll(topRow, bottomRow);

            // Add hover effect
            itemContainer.setOnMouseEntered(e -> {
                itemContainer.setStyle(itemContainer.getStyle() + " -fx-background-color: #f8fafc; -fx-background-radius: 4;");
            });

            itemContainer.setOnMouseExited(e -> {
                itemContainer.setStyle(itemContainer.getStyle().replace(" -fx-background-color: #f8fafc; -fx-background-radius: 4;", ""));
            });

            // Add click handler for future navigation
            itemContainer.setOnMouseClicked(e -> {
                try {
                    // TODO: Navigate to related item based on activity type
                    String message = "Clicked on: " + activity.getTitle();
                    System.out.println("Activity clicked: " + activity.getType() + " - " + activity.getTitle());
                } catch (Exception ex) {
                    System.err.println("Error handling activity click: " + ex.getMessage());
                }
            });

            return itemContainer;

        } catch (Exception e) {
            System.err.println("Error creating activity item UI: " + e.getMessage());
            // Fallback simple item
            VBox fallback = new VBox();
            Label fallbackLabel = new Label(activity.getIcon() + " " + activity.getTitle());
            fallbackLabel.setStyle("-fx-font-size: 11px; -fx-padding: 4;");
            fallback.getChildren().add(fallbackLabel);
            return fallback;
        }
    }

    private void addActivityItemAnimation(VBox activityItem, double delayMs) {
        try {
            activityItem.setOpacity(0);
            activityItem.setTranslateX(-10);

            PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(200), activityItem);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(200), activityItem);
                slide.setFromX(-10);
                slide.setToX(0);
                slide.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition animation = new ParallelTransition(fade, slide);
                animation.play();
            });
            delay.play();
        } catch (Exception e) {
            // Fallback: just show the item
            activityItem.setOpacity(1);
            activityItem.setTranslateX(0);
        }
    }

    private void clearDashboardStats() {
        // Tasks
        if (tasksCompletedLabel != null) tasksCompletedLabel.setText("No tasks completed");
        if (focusTimeLabel != null) focusTimeLabel.setText("No focus time recorded");

        // Productivity Insights
        if (streakLabel != null) streakLabel.setText("No active streak");
        if (goalsProgressLabel != null) goalsProgressLabel.setText("No goals set");
        if (moodAverageLabel != null) moodAverageLabel.setText("No mood entries");
        if (efficiencyLabel != null) efficiencyLabel.setText("--");
        if (todayProgressLabel != null) todayProgressLabel.setText("No progress yet");
        if (recentActivityLabel != null) recentActivityLabel.setText("No recent activity");

        // Reset progress bars
        if (tasksProgress != null) tasksProgress.setProgress(0.0);
        if (focusProgress != null) focusProgress.setProgress(0.0);
        if (goalsProgress != null) goalsProgress.setProgress(0.0);
        if (moodProgress != null) moodProgress.setProgress(0.0);
        if (todayTasksProgress != null) todayTasksProgress.setProgress(0.0);
        if (todayFocusProgress != null) todayFocusProgress.setProgress(0.0);
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
                    loadRealDashboardData();
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

    private void startPeriodicUpdates() {
        try {
            // Update dashboard data every 5 minutes
            Timeline updateTimeline = new Timeline(
                    new KeyFrame(Duration.minutes(5), e -> loadRealDashboardData())
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
                loadRealDashboardData();
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

    private void setupUserProfileSection() {
        try {
            // Update tampilan profile dengan data user
            updateUserProfileDisplay();

            // Setup click handler yang aman
            if (userProfileSection != null) {
                // Gunakan method yang aman tanpa animasi
                userProfileSection.setOnMouseClicked(e -> handleProfileClickSimple());

                // Tambahkan visual feedback sederhana
                userProfileSection.setOnMouseEntered(e -> {
                    userProfileSection.getStyleClass().add("profile-hover");
                });

                userProfileSection.setOnMouseExited(e -> {
                    userProfileSection.getStyleClass().remove("profile-hover");
                });

                System.out.println("✅ User profile section setup completed (safe mode)");
            }

        } catch (Exception e) {
            ErrorHandler.handleError("Profile Setup", "Failed to setup user profile section", e);
        }
    }

    /**
     * ✅ TAMBAH METHOD INI - Update tampilan profile dengan data user real
     */
    private void updateUserProfileDisplay() {
        try {
            User currentUser = UserSession.getInstance().getCurrentUser();
            if (currentUser == null) {
                System.out.println("⚠️ No current user found");
                return;
            }

            // Cari label profile name di sidebar
            Label profileNameLabel = findLabelInSidebar(".profile-name");
            if (profileNameLabel != null) {
                String displayName = currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()
                        ? currentUser.getFullName()
                        : currentUser.getUsername();
                profileNameLabel.setText(displayName);
            }

            // Cari label profile level di sidebar
            Label profileLevelLabel = findLabelInSidebar(".profile-level");
            if (profileLevelLabel != null) {
                if (currentUser instanceof RegularUser) {
                    String formattedDate = ((RegularUser) currentUser).getFormattedJoinDate();
                    profileLevelLabel.setText(formattedDate + " ⭐");
                } else {
                    profileLevelLabel.setText("Active Member ⭐");
                }
            }

            System.out.println("✅ User profile display updated for: " + currentUser.getUsername());

        } catch (Exception e) {
            System.err.println("Error updating user profile display: " + e.getMessage());
        }
    }

    /**
     * ✅ TAMBAH METHOD INI - Handle klik pada profile section
     */
    private void handleProfileClick() {
        try {
            System.out.println("👤 Profile section clicked - Opening profile dialog");

            // Langsung buka dialog tanpa animasi untuk menghindari konflik
            Platform.runLater(() -> {
                try {
                    showProfileDialog();
                } catch (Exception e) {
                    ErrorHandler.handleError("Profile Dialog", "Failed to show profile dialog", e);
                }
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Profile Click", "Failed to handle profile click", e);
        }
    }

    /**
     * ✅ TAMBAH METHOD INI - Show dialog profile
     */
    private void showProfileDialog() {
        try {
            // Load FXML profile dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile-dialog.fxml"));
            VBox profileDialog = loader.load();

            // Get controller
            ProfileDialogController controller = loader.getController();

            // Buat stage baru untuk dialog
            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.initOwner(dashboardContainer.getScene().getWindow());
            dialogStage.setTitle("User Profile");
            dialogStage.setResizable(true); // ✅ CHANGED: Allow resizing

            // Get screen dimensions for responsive sizing
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenHeight = screenBounds.getHeight();
            double screenWidth = screenBounds.getWidth();

            // Calculate dialog size based on screen size
            double dialogHeight = Math.min(580, screenHeight * 0.85);
            double dialogWidth = Math.min(460, screenWidth * 0.9);

            // Set minimum and maximum sizes
            dialogStage.setMinWidth(400);
            dialogStage.setMinHeight(350);
            dialogStage.setMaxWidth(500);
            dialogStage.setMaxHeight(Math.max(600, screenHeight * 0.9));

            // Buat scene dengan ukuran yang responsif
            Scene scene = new Scene(profileDialog, dialogWidth, dialogHeight);

            // Apply tema yang sedang aktif
            try {
                ThemeManager themeManager = ThemeManager.getInstance();
                if (themeManager.getCurrentTheme() != null) {
                    String cssPath = "/css/" + themeManager.getCurrentTheme().getCssFile();
                    scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not apply theme to profile dialog: " + e.getMessage());
            }

            dialogStage.setScene(scene);

            // Set dialog stage ke controller
            controller.setDialogStage(dialogStage);

            // Setup responsive dialog
            controller.setupResponsiveDialog(dialogStage);

            // Center dialog pada parent window dengan bounds checking
            Stage parentStage = (Stage) dashboardContainer.getScene().getWindow();
            if (parentStage != null) {
                double centerX = parentStage.getX() + (parentStage.getWidth() - dialogWidth) / 2;
                double centerY = parentStage.getY() + (parentStage.getHeight() - dialogHeight) / 2;

                // Ensure dialog stays within screen bounds
                centerX = Math.max(0, Math.min(centerX, screenBounds.getWidth() - dialogWidth));
                centerY = Math.max(0, Math.min(centerY, screenBounds.getHeight() - dialogHeight));

                dialogStage.setX(centerX);
                dialogStage.setY(centerY);
            }

            // Show dialog
            dialogStage.show();

            // Refresh data user setelah dialog ditutup
            dialogStage.setOnHidden(e -> {
                Platform.runLater(() -> {
                    updateUserProfileDisplay();
                });
            });

            System.out.println("✅ Responsive profile dialog opened successfully");

        } catch (Exception e) {
            ErrorHandler.handleError("Profile Dialog", "Failed to show profile dialog", e);

            // Fallback: show alert sederhana
            Platform.runLater(() -> {
                showFallbackProfileInfo();
            });
        }
    }

    /**
     * ✅ TAMBAH METHOD INI - Fallback jika dialog gagal
     */
    private void showFallbackProfileInfo() {
        Alert fallbackAlert = new Alert(Alert.AlertType.INFORMATION);
        fallbackAlert.setTitle("User Profile");
        fallbackAlert.setHeaderText("Profile Information");

        User currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            String content = "Username: " + currentUser.getUsername() + "\n" +
                    "Full Name: " + (currentUser.getFullName() != null ? currentUser.getFullName() : "Not set") + "\n" +
                    "Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "Not set");
            fallbackAlert.setContentText(content);
        } else {
            fallbackAlert.setContentText("No user information available.");
        }

        fallbackAlert.showAndWait();
    }

    /**
     * ✅ TAMBAH METHOD INI - Helper method untuk cari label di sidebar
     */
    private Label findLabelInSidebar(String selector) {
        try {
            if (sidebar != null) {
                return (Label) sidebar.lookup(selector);
            }
            return null;
        } catch (Exception e) {
            System.err.println("Error finding label: " + selector + " - " + e.getMessage());
            return null;
        }
    }

    private void handleProfileClickSimple() {
        try {
            System.out.println("👤 Profile section clicked - Opening profile dialog (simple)");
            showProfileDialog();
        } catch (Exception e) {
            ErrorHandler.handleError("Profile Click", "Failed to handle profile click", e);
            showFallbackProfileInfo();
        }
    }
}