package com.focusbuddy.controllers;

import com.focusbuddy.models.Goal;
import com.focusbuddy.models.StudyGoal;
import com.focusbuddy.models.FocusGoal;
import com.focusbuddy.services.GoalsService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
    
    @FXML
    private void initialize() {
        goalsService = new GoalsService();
        
        setupGoalsList();
        setupComboBox();
        setupButtons();
        setupSpinner();
        loadGoals();
        loadStatistics();
        loadAchievements();
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
        String title = goalTitleField.getText().trim();
        String description = goalDescriptionArea.getText().trim();
        Goal.GoalType type = goalTypeCombo.getValue();
        int targetValue = targetValueSpinner.getValue();
        LocalDate targetDate = targetDatePicker.getValue();
        
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
            // Create new goal based on type
            goal = switch (type) {
                case STUDY_HOURS -> new StudyGoal(title, description, targetValue);
                case FOCUS_SESSIONS -> new FocusGoal(title, description, targetValue);
                case TASKS_COMPLETED -> new StudyGoal(title, description, targetValue); // Can create TaskGoal class
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
        } else {
            success = goalsService.updateGoal(goal);
        }
        
        if (success) {
            NotificationManager.getInstance().showNotification(
                "Goal Saved", 
                "Your goal has been saved successfully!", 
                NotificationManager.NotificationType.SUCCESS
            );
            loadGoals();
            loadStatistics();
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
    
    private void loadStatistics() {
        int userId = UserSession.getInstance().getCurrentUser().getId();
        
        int totalGoals = goalsService.getTotalGoalsCount(userId);
        int completedGoals = goalsService.getCompletedGoalsCount(userId);
        int activeGoals = goalsService.getActiveGoalsCount(userId);
        
        totalGoalsLabel.setText("Total Goals: " + totalGoals);
        completedGoalsLabel.setText("Completed: " + completedGoals);
        activeGoalsLabel.setText("Active: " + activeGoals);
    }
    
    private void loadAchievements() {
        achievementsContainer.getChildren().clear();
        
        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<String> achievements = goalsService.getUserAchievements(userId);
        
        for (String achievement : achievements) {
            Label achievementLabel = new Label("🏆 " + achievement);
            achievementLabel.setStyle("-fx-font-size: 14px; -fx-padding: 5px; " +
                                    "-fx-background-color: #fff3cd; -fx-background-radius: 5px;");
            achievementsContainer.getChildren().add(achievementLabel);
        }
    }
}
