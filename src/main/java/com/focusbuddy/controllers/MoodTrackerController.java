package com.focusbuddy.controllers;

import com.focusbuddy.models.MoodEntry;
import com.focusbuddy.services.MoodService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

public class MoodTrackerController {
    
    @FXML private VBox moodContainer;
    @FXML private Slider moodSlider;
    @FXML private Label moodLabel;
    @FXML private Label moodEmoji;
    @FXML private TextArea moodDescription;
    @FXML private Button saveMoodButton;
    @FXML private LineChart<String, Number> moodChart;
    @FXML private VBox moodHistory;
    @FXML private Label averageMoodLabel;
    @FXML private Label moodStreakLabel;
    
    private MoodService moodService;

    @FXML
    private void initialize() {
        try {
            moodService = new MoodService();

            setupMoodSlider();
            setupSaveButton();

            // ✅ UBAH: Load data real
            loadMoodData();
            loadMoodChart();

            System.out.println("✅ Mood tracker initialized successfully");
        } catch (Exception e) {
            System.err.println("Error initializing mood tracker: " + e.getMessage());
            showEmptyState(); // ✅ TAMBAH
        }
    }
    
    private void setupMoodSlider() {
        moodSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int moodLevel = newVal.intValue();
            updateMoodDisplay(moodLevel);
        });
        
        // Set initial value
        moodSlider.setValue(3);
        updateMoodDisplay(3);
    }
    
    private void updateMoodDisplay(int moodLevel) {
        String[] moodTexts = {"Very Sad", "Sad", "Neutral", "Happy", "Very Happy"};
        String[] moodEmojis = {"😢", "😕", "😐", "😊", "😄"};
        
        moodLabel.setText(moodTexts[moodLevel - 1]);
        moodEmoji.setText(moodEmojis[moodLevel - 1]);
    }
    
    private void setupSaveButton() {
        saveMoodButton.setOnAction(e -> saveMoodEntry());
    }
    
    private void saveMoodEntry() {
        int moodLevel = (int) moodSlider.getValue();
        String description = moodDescription.getText().trim();
        
        MoodEntry entry = new MoodEntry(
            UserSession.getInstance().getCurrentUser().getId(),
            moodLevel,
            description
        );
        
        if (moodService.saveMoodEntry(entry)) {
            NotificationManager.getInstance().showNotification(
                "Mood Saved", 
                "Your mood has been recorded successfully!", 
                NotificationManager.NotificationType.SUCCESS
            );
            
            moodDescription.clear();
            loadMoodData();
            loadMoodChart();
        } else {
            NotificationManager.getInstance().showNotification(
                "Error", 
                "Failed to save mood entry", 
                NotificationManager.NotificationType.ERROR
            );
        }
    }

    // ✅ UBAH NAMA dari clearMoodStats() ke showEmptyState()
    private void showEmptyState() {
        if (averageMoodLabel != null) {
            averageMoodLabel.setText("No mood entries yet - start tracking!");
        }
        if (moodStreakLabel != null) {
            moodStreakLabel.setText("Log your mood daily to build a streak");
        }
        if (moodHistory != null) {
            moodHistory.getChildren().clear();
            Label emptyLabel = new Label("🌈 Start tracking your mood to see patterns and insights!");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            moodHistory.getChildren().add(emptyLabel);
        }
        if (moodChart != null) {
            moodChart.getData().clear();
        }
    }

    private void loadMoodData() {
        try {
            int userId = UserSession.getInstance().getCurrentUser().getId();
            List<MoodEntry> recentEntries = moodService.getRecentMoodEntries(userId, 7);

            if (!recentEntries.isEmpty()) {
                double averageMood = recentEntries.stream()
                        .mapToInt(MoodEntry::getMoodLevel)
                        .average()
                        .orElse(0.0);

                averageMoodLabel.setText(String.format("Average: %.1f/5", averageMood));

                int streak = moodService.getMoodStreak(userId);
                if (streak > 0) {
                    moodStreakLabel.setText("Streak: " + streak + " days 🔥");
                } else {
                    moodStreakLabel.setText("Start your mood tracking streak!");
                }

                updateMoodHistory(recentEntries);
            } else {
                // ✅ TAMBAH: Show empty state untuk user baru
                showEmptyState();
            }
        } catch (Exception e) {
            System.err.println("Error loading mood data: " + e.getMessage());
            showEmptyState(); // ✅ TAMBAH
        }
    }
    
    private void updateMoodHistory(List<MoodEntry> entries) {
        moodHistory.getChildren().clear();
        
        for (MoodEntry entry : entries) {
            VBox entryBox = new VBox(5);
            entryBox.setStyle("-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-background-radius: 8;");
            
            Label dateLabel = new Label(entry.getEntryDate().toString());
            dateLabel.setStyle("-fx-font-weight: bold;");
            
            Label moodLabel = new Label(entry.getMoodEmoji() + " " + entry.getMoodLevel() + "/5");
            moodLabel.setStyle("-fx-font-size: 16px;");
            
            if (entry.getMoodDescription() != null && !entry.getMoodDescription().isEmpty()) {
                Label descLabel = new Label(entry.getMoodDescription());
                descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
                descLabel.setWrapText(true);
                entryBox.getChildren().add(descLabel);
            }
            
            entryBox.getChildren().addAll(dateLabel, moodLabel);
            moodHistory.getChildren().add(entryBox);
        }
    }
    
    private void loadMoodChart() {
        int userId = UserSession.getInstance().getCurrentUser().getId();
        List<MoodEntry> entries = moodService.getMoodEntriesForChart(userId, 30);
        
        moodChart.getData().clear();
        
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Mood Level");
        
        for (MoodEntry entry : entries) {
            series.getData().add(new XYChart.Data<>(
                entry.getEntryDate().toString(), 
                entry.getMoodLevel()
            ));
        }
        
        moodChart.getData().add(series);
    }
}
