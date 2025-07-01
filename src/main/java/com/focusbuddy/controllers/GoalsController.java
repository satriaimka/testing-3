package com.focusbuddy.controllers;

import com.focusbuddy.models.Goal;
import com.focusbuddy.models.StudyGoal;
import com.focusbuddy.models.FocusGoal;
import com.focusbuddy.models.TaskGoal;
import com.focusbuddy.services.GoalsService;
import com.focusbuddy.services.GoalProgressManager;
import com.focusbuddy.observers.AchievementObserver;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.animation.ParallelTransition;
import javafx.util.Duration;
import javafx.geometry.Pos;

import java.time.LocalDate;
import java.util.List;

public class GoalsController {
    
    @FXML private VBox goalsContainer;
    @FXML private ListView<Goal> goalsList;
    @FXML private TextField goalTitleField;
    @FXML private TextArea goalDescriptionArea;
    @FXML private ComboBox<Goal.GoalType> goalTypeCombo;
    @FXML private Spinner<Integer> targetValueSpinner;
    @FXML private DatePicker targetDatePicker;
    @FXML private Button saveGoalButton;
    @FXML private Button newGoalButton;
    @FXML private Button deleteGoalButton;
    @FXML private VBox achievementsContainer;
    @FXML private Label totalGoalsLabel;
    @FXML private Label completedGoalsLabel;
    @FXML private Label activeGoalsLabel;
    
    private GoalsService goalsService;
    private Goal currentGoal;
    private GoalProgressManager goalProgressManager;
    private AchievementObserver achievementObserver;

    @FXML
    private void initialize() {
        try {
            goalsService = new GoalsService();

            // ✅ NEW: Initialize goal progress manager dan observer
            initializeGoalSystem();

            setupGoalsList();
            setupComboBox();
            setupButtons();
            setupSpinner();

            // Load data real, jangan clear
            loadGoals();
            loadStatistics();
            loadAchievements();

            System.out.println("✅ Goals controller initialized successfully with auto-progress system");
        } catch (Exception e) {
            System.err.println("Error initializing goals controller: " + e.getMessage());
            showEmptyState();
        }
    }
    
    private void setupGoalsList() {
        goalsList.setCellFactory(listView -> new ListCell<Goal>() {
            @Override
            protected void updateItem(Goal goal, boolean empty) {
                super.updateItem(goal, empty);
                if (empty || goal == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox content = new VBox(5);
                    
                    Label titleLabel = new Label(goal.getTitle());
                    titleLabel.setStyle("-fx-font-weight: bold;");
                    
                    ProgressBar progressBar = new ProgressBar(goal.getProgressPercentage() / 100.0);
                    progressBar.setPrefWidth(200);
                    
                    Label progressLabel = new Label(String.format("%.1f%% (%d/%d)", 
                        goal.getProgressPercentage(), goal.getCurrentValue(), goal.getTargetValue()));
                    
                    Label statusLabel = new Label(goal.getStatus().toString());
                    statusLabel.setStyle(getStatusStyle(goal.getStatus()));
                    
                    content.getChildren().addAll(titleLabel, progressBar, progressLabel, statusLabel);
                    setGraphic(content);
                }
            }
        });
        
        goalsList.getSelectionModel().selectedItemProperty().addListener((obs, oldGoal, newGoal) -> {
            if (newGoal != null) {
                loadGoalDetails(newGoal);
            }
        });
    }
    
    private String getStatusStyle(Goal.Status status) {
        return switch (status) {
            case COMPLETED -> "-fx-text-fill: #4CAF50; -fx-font-weight: bold;";
            case ACTIVE -> "-fx-text-fill: #2196F3; -fx-font-weight: bold;";
            case PAUSED -> "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
        };
    }
    
    private void setupComboBox() {
        goalTypeCombo.getItems().addAll(Goal.GoalType.values());
        goalTypeCombo.setValue(Goal.GoalType.STUDY_HOURS);
    }
    
    private void setupButtons() {
        newGoalButton.setOnAction(e -> createNewGoal());
        saveGoalButton.setOnAction(e -> saveCurrentGoal());
        deleteGoalButton.setOnAction(e -> deleteCurrentGoal());
    }
    
    private void setupSpinner() {
        targetValueSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 1000, 10));
    }
    
    private void loadGoals() {
        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<Goal> goals = goalsService.getGoalsForUser(userId);
        goalsList.getItems().setAll(goals);
    }
    
    private void loadGoalDetails(Goal goal) {
        currentGoal = goal;
        goalTitleField.setText(goal.getTitle());
        goalDescriptionArea.setText(goal.getDescription());
        goalTypeCombo.setValue(goal.getGoalType());
        targetValueSpinner.getValueFactory().setValue(goal.getTargetValue());
        targetDatePicker.setValue(goal.getTargetDate());
    }
    
    private void createNewGoal() {
        currentGoal = null;
        goalTitleField.clear();
        goalDescriptionArea.clear();
        goalTypeCombo.setValue(Goal.GoalType.STUDY_HOURS);
        targetValueSpinner.getValueFactory().setValue(10);
        targetDatePicker.setValue(LocalDate.now().plusWeeks(1));
        
        goalsList.getSelectionModel().clearSelection();
    }

    private void saveCurrentGoal() {
        String title = goalTitleField != null ? goalTitleField.getText().trim() : "";
        String description = goalDescriptionArea != null ? goalDescriptionArea.getText().trim() : "";
        Goal.GoalType type = goalTypeCombo != null ? goalTypeCombo.getValue() : Goal.GoalType.STUDY_HOURS;
        int targetValue = targetValueSpinner != null ? targetValueSpinner.getValue() : 10;
        LocalDate targetDate = targetDatePicker != null ? targetDatePicker.getValue() : LocalDate.now().plusWeeks(1);

        if (title.isEmpty()) {
            NotificationManager.getInstance().showNotification(
                    "Validation Error",
                    "Goal title cannot be empty",
                    NotificationManager.NotificationType.WARNING
            );
            return;
        }

        Goal goal;
        if (currentGoal == null) {
            // ✅ UPDATED: Create new goal based on type dengan TaskGoal
            goal = switch (type) {
                case STUDY_HOURS -> new StudyGoal(title, description, targetValue);
                case FOCUS_SESSIONS -> new FocusGoal(title, description, targetValue);
                case TASKS_COMPLETED -> new TaskGoal(title, description, targetValue); // ✅ USE TaskGoal
            };
            goal.setUserId(UserSession.getInstance().getCurrentUser().getId());
        } else {
            goal = currentGoal;
            goal.setTitle(title);
            goal.setDescription(description);
            goal.setTargetValue(targetValue);
        }

        goal.setTargetDate(targetDate);

        boolean success;
        if (currentGoal == null) {
            success = goalsService.createGoal(goal);

            // ✅ NEW: Notify goal creation untuk observer
            if (success && goalProgressManager != null) {
                try {
                    // Observer akan otomatis dipanggil dari GoalsService.createGoal()
                    System.out.println("✅ Goal creation notification sent to observers");

                } catch (Exception e) {
                    System.err.println("⚠️ Error notifying goal creation: " + e.getMessage());
                }
            }
        } else {
            success = goalsService.updateGoal(goal);
        }

        if (success) {
            String action = currentGoal == null ? "created" : "updated";
            String message = currentGoal == null ?
                    "Your goal has been created! Start working towards it! 🎯" :
                    "Your goal has been updated successfully! 📝";

            NotificationManager.getInstance().showNotification(
                    "Goal " + (currentGoal == null ? "Created" : "Updated"),
                    message,
                    NotificationManager.NotificationType.SUCCESS
            );

            loadGoals();
            loadStatistics();

            // ✅ NEW: Show goal creation tips untuk new goals
            if (currentGoal == null) {
                showGoalCreationTips(goal);
            }

        } else {
            NotificationManager.getInstance().showNotification(
                    "Error",
                    "Failed to save goal",
                    NotificationManager.NotificationType.ERROR
            );
        }
    }
    
    private void deleteCurrentGoal() {
        if (currentGoal == null) {
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Goal");
        alert.setHeaderText("Are you sure you want to delete this goal?");
        alert.setContentText("This action cannot be undone.");
        
        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                if (goalsService.deleteGoal(currentGoal.getId())) {
                    NotificationManager.getInstance().showNotification(
                        "Goal Deleted", 
                        "Goal has been deleted successfully", 
                        NotificationManager.NotificationType.SUCCESS
                    );
                    loadGoals();
                    loadStatistics();
                    createNewGoal();
                }
            }
        });
    }

    // ✅ UBAH NAMA dari clearGoalsStats() ke showEmptyState()
    private void showEmptyState() {
        if (totalGoalsLabel != null) totalGoalsLabel.setText("No goals set yet");
        if (completedGoalsLabel != null) completedGoalsLabel.setText("Start by creating your first goal");
        if (activeGoalsLabel != null) activeGoalsLabel.setText("Set goals to track your progress");

        if (achievementsContainer != null) {
            achievementsContainer.getChildren().clear();
            Label emptyLabel = new Label("🎯 Create goals and complete them to unlock achievements!");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            achievementsContainer.getChildren().add(emptyLabel);
        }

        if (goalsList != null) {
            goalsList.getItems().clear();
            goalsList.setPlaceholder(new Label("Create your first goal to start tracking progress!"));
        }
    }

    private void loadStatistics() {
        try {
            int userId = UserSession.getInstance().getCurrentUser().getId();

            int totalGoals = goalsService.getTotalGoalsCount(userId);
            int completedGoals = goalsService.getCompletedGoalsCount(userId);
            int activeGoals = goalsService.getActiveGoalsCount(userId);

            if (totalGoals > 0) {
                // ✅ ENHANCED: Show more detailed statistics
                totalGoalsLabel.setText("Total Goals: " + totalGoals);
                completedGoalsLabel.setText("Completed: " + completedGoals + " (" +
                        String.format("%.0f%%", (double)completedGoals/totalGoals*100) + ")");
                activeGoalsLabel.setText("Active: " + activeGoals);

                // ✅ NEW: Show goal progress insights
                showGoalProgressInsights(userId);

            } else {
                showEmptyState();
            }
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            showEmptyState();
        }
    }

    private void loadAchievements() {
        achievementsContainer.getChildren().clear();

        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<String> achievements = goalsService.getUserAchievements(userId);

        if (!achievements.isEmpty()) {
            // ✅ ENHANCED: Better achievement display dengan animations
            for (int i = 0; i < achievements.size(); i++) {
                String achievement = achievements.get(i);

                HBox achievementItem = createAchievementItem(achievement);
                achievementsContainer.getChildren().add(achievementItem);

                // Add entrance animation dengan delay
                addAchievementAnimation(achievementItem, i * 100);
            }

            // ✅ NEW: Add progress summary
            if (goalProgressManager != null) {
                try {
                    double completionRate = goalProgressManager.getGoalCompletionRate(userId);
                    if (completionRate > 0) {
                        Label progressSummary = new Label(
                                String.format("🎯 Overall Progress: %.0f%% completion rate", completionRate)
                        );
                        progressSummary.setStyle("-fx-text-fill: #667eea; -fx-font-size: 12px; -fx-padding: 10 5 5 5; -fx-font-weight: bold;");
                        achievementsContainer.getChildren().add(0, progressSummary);
                    }
                } catch (Exception e) {
                    System.err.println("Error adding progress summary: " + e.getMessage());
                }
            }

        } else {
            Label emptyLabel = new Label("Complete goals to earn achievements!");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            achievementsContainer.getChildren().add(emptyLabel);
        }
    }

    private HBox createAchievementItem(String achievement) {
        HBox item = new HBox(8);
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setStyle("-fx-padding: 8; -fx-background-color: #fff3cd; -fx-background-radius: 8; -fx-border-color: #ffeaa7; -fx-border-radius: 8;");

        Label icon = new Label("🏆");
        icon.setStyle("-fx-font-size: 14px;");

        Label text = new Label(achievement);
        text.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        text.setWrapText(true);

        item.getChildren().addAll(icon, text);
        return item;
    }

    private void addAchievementAnimation(HBox achievementItem, double delayMs) {
        try {
            achievementItem.setOpacity(0);
            achievementItem.setTranslateX(-10);

            PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
            delay.setOnFinished(e -> {
                FadeTransition fade = new FadeTransition(Duration.millis(300), achievementItem);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(300), achievementItem);
                slide.setFromX(-10);
                slide.setToX(0);

                ParallelTransition animation = new ParallelTransition(fade, slide);
                animation.play();
            });
            delay.play();
        } catch (Exception e) {
            // Fallback: just show the item
            achievementItem.setOpacity(1);
            achievementItem.setTranslateX(0);
        }
    }

    private void initializeGoalSystem() {
        try {
            // Initialize goal progress manager
            goalProgressManager = GoalProgressManager.getInstance();

            // Create dan register achievement observer
            achievementObserver = new AchievementObserver();
            goalProgressManager.addObserver(achievementObserver);

            System.out.println("✅ Goal progress system initialized with achievement observer");

        } catch (Exception e) {
            System.err.println("⚠️ Error initializing goal system: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showGoalCreationTips(Goal goal) {
        try {
            String tips = getGoalTypeTips(goal.getGoalType());

            // Show tips in a delayed notification
            Platform.runLater(() -> {
                try {
                    Thread.sleep(2000); // Wait 2 seconds

                    NotificationManager.getInstance().showNotification(
                            "💡 Goal Tips",
                            tips,
                            NotificationManager.NotificationType.INFO
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

        } catch (Exception e) {
            System.err.println("Error showing goal creation tips: " + e.getMessage());
        }
    }

    private String getGoalTypeTips(Goal.GoalType goalType) {
        return switch (goalType) {
            case TASKS_COMPLETED ->
                    "🎯 Task Completion Goal Tips:\n\n" +
                            "• Your goal will automatically progress when you complete tasks\n" +
                            "• Focus on marking tasks as 'Completed' in the Tasks section\n" +
                            "• Break large tasks into smaller, manageable ones\n" +
                            "• Check your progress regularly to stay motivated!";

            case FOCUS_SESSIONS ->
                    "🍅 Focus Session Goal Tips:\n\n" +
                            "• Your goal will automatically progress when you complete Pomodoro sessions\n" +
                            "• Use the Focus Timer on the dashboard to start sessions\n" +
                            "• Even 25-minute sessions count towards your goal\n" +
                            "• Consistency is key - aim for regular focus sessions!";

            case STUDY_HOURS ->
                    "📚 Study Hours Goal Tips:\n\n" +
                            "• Your goal will automatically progress based on completed focus sessions\n" +
                            "• Each 25+ minute focus session counts as 1 hour\n" +
                            "• Use the Pomodoro timer to track your study time\n" +
                            "• Quality over quantity - focused study is more effective!";
        };
    }

    private void showGoalProgressInsights(int userId) {
        try {
            if (goalProgressManager != null) {
                // Get goal completion rate
                double completionRate = goalProgressManager.getGoalCompletionRate(userId);

                // Get near completion goals
                List<Goal> nearCompletionGoals = goalProgressManager.getNearCompletionGoals(userId);

                // Get daily summary
                String dailySummary = goalProgressManager.getDailyGoalSummary(userId);

                System.out.println("📊 Goal Insights:");
                System.out.println("   - Completion Rate: " + String.format("%.1f%%", completionRate));
                System.out.println("   - Near Completion: " + nearCompletionGoals.size() + " goals");
                System.out.println("   - Daily Summary: " + dailySummary);

                // Could display these insights in UI if needed

            }
        } catch (Exception e) {
            System.err.println("Error showing goal progress insights: " + e.getMessage());
        }
    }

    public void cleanup() {
        try {
            if (goalProgressManager != null && achievementObserver != null) {
                goalProgressManager.removeObserver(achievementObserver);
                System.out.println("✅ Goal observer cleaned up successfully");
            }
        } catch (Exception e) {
            System.err.println("Error cleaning up goal observer: " + e.getMessage());
        }
    }
}