package com.focusbuddy.controllers;

import com.focusbuddy.services.ExportService;
import com.focusbuddy.utils.NotificationManager;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class ExportController {
    
    @FXML private Button exportCSVButton;
    @FXML private Button backupDatabaseButton;
    @FXML private TextField exportPathField;
    @FXML private Button browseButton;
    @FXML private ProgressBar exportProgress;
    @FXML private Label statusLabel;
    @FXML private CheckBox includeTasksCheckBox;
    @FXML private CheckBox includeMoodCheckBox;
    @FXML private CheckBox includeNotesCheckBox;
    @FXML private CheckBox includeGoalsCheckBox;
    
    private ExportService exportService;
    
    @FXML
    private void initialize() {
        exportService = new ExportService();
        
        setupButtons();
        setupCheckBoxes();
        
        // Set default export path
        exportPathField.setText(System.getProperty("user.home") + "/FocusBuddy_Export");
    }
    
    private void setupButtons() {
        browseButton.setOnAction(e -> browseForDirectory());
        exportCSVButton.setOnAction(e -> exportToCSV());
        backupDatabaseButton.setOnAction(e -> backupDatabase());
    }
    
    private void setupCheckBoxes() {
        // Select all by default
        includeTasksCheckBox.setSelected(true);
        includeMoodCheckBox.setSelected(true);
        includeNotesCheckBox.setSelected(true);
        includeGoalsCheckBox.setSelected(true);
    }
    
    private void browseForDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Export Directory");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        Stage stage = (Stage) browseButton.getScene().getWindow();
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        if (selectedDirectory != null) {
            exportPathField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    private void exportToCSV() {
        String exportPath = exportPathField.getText().trim();
        
        if (exportPath.isEmpty()) {
            NotificationManager.getInstance().showNotification(
                "Export Error", 
                "Please select an export directory", 
                NotificationManager.NotificationType.WARNING
            );
            return;
        }
        
        // Create directory if it doesn't exist
        File exportDir = new File(exportPath);
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        
        // Disable buttons during export
        setExportControlsEnabled(false);
        exportProgress.setVisible(true);
        statusLabel.setText("Exporting data...");
        
        // Create background task for export
        Task<Boolean> exportTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return exportService.exportAllDataToCSV(exportPath).get();
            }
            
            @Override
            protected void succeeded() {
                setExportControlsEnabled(true);
                exportProgress.setVisible(false);
                
                if (getValue()) {
                    statusLabel.setText("Export completed successfully!");
                    NotificationManager.getInstance().showNotification(
                        "Export Complete", 
                        "Your data has been exported to: " + exportPath, 
                        NotificationManager.NotificationType.SUCCESS
                    );
                } else {
                    statusLabel.setText("Export failed!");
                    NotificationManager.getInstance().showNotification(
                        "Export Failed", 
                        "Failed to export data. Please try again.", 
                        NotificationManager.NotificationType.ERROR
                    );
                }
            }
            
            @Override
            protected void failed() {
                setExportControlsEnabled(true);
                exportProgress.setVisible(false);
                statusLabel.setText("Export failed!");
                
                NotificationManager.getInstance().showNotification(
                    "Export Failed", 
                    "An error occurred during export: " + getException().getMessage(), 
                    NotificationManager.NotificationType.ERROR
                );
            }
        };
        
        Thread exportThread = new Thread(exportTask);
        exportThread.setDaemon(true);
        exportThread.start();
    }
    
    private void backupDatabase() {
        String backupPath = exportPathField.getText().trim();
        
        if (backupPath.isEmpty()) {
            NotificationManager.getInstance().showNotification(
                "Backup Error", 
                "Please select a backup directory", 
                NotificationManager.NotificationType.WARNING
            );
            return;
        }
        
        // Create directory if it doesn't exist
        File backupDir = new File(backupPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
        
        // Disable buttons during backup
        setExportControlsEnabled(false);
        exportProgress.setVisible(true);
        statusLabel.setText("Creating database backup...");
        
        // Create background task for backup
        Task<Boolean> backupTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return exportService.backupDatabase(backupPath).get();
            }
            
            @Override
            protected void succeeded() {
                setExportControlsEnabled(true);
                exportProgress.setVisible(false);
                
                if (getValue()) {
                    statusLabel.setText("Database backup completed successfully!");
                    NotificationManager.getInstance().showNotification(
                        "Backup Complete", 
                        "Database backup has been created in: " + backupPath, 
                        NotificationManager.NotificationType.SUCCESS
                    );
                } else {
                    statusLabel.setText("Database backup failed!");
                    NotificationManager.getInstance().showNotification(
                        "Backup Failed", 
                        "Failed to create database backup. Please check your MySQL installation.", 
                        NotificationManager.NotificationType.ERROR
                    );
                }
            }
            
            @Override
            protected void failed() {
                setExportControlsEnabled(true);
                exportProgress.setVisible(false);
                statusLabel.setText("Database backup failed!");
                
                NotificationManager.getInstance().showNotification(
                    "Backup Failed", 
                    "An error occurred during backup: " + getException().getMessage(), 
                    NotificationManager.NotificationType.ERROR
                );
            }
        };
        
        Thread backupThread = new Thread(backupTask);
        backupThread.setDaemon(true);
        backupThread.start();
    }
    
    private void setExportControlsEnabled(boolean enabled) {
        exportCSVButton.setDisable(!enabled);
        backupDatabaseButton.setDisable(!enabled);
        browseButton.setDisable(!enabled);
    }
}
