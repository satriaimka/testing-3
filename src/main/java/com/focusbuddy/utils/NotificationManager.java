package com.focusbuddy.utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

public class NotificationManager {
    private static NotificationManager instance;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    
    private NotificationManager() {
        initializeSystemTray();
    }
    
    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
    
    private void initializeSystemTray() {
        if (SystemTray.isSupported()) {
            systemTray = SystemTray.getSystemTray();
            
            // Create tray icon
            Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
            trayIcon = new TrayIcon(image, "FocusBuddy");
            trayIcon.setImageAutoSize(true);
            trayIcon.setToolTip("FocusBuddy - Productivity Assistant");
            
            try {
                systemTray.add(trayIcon);
            } catch (AWTException e) {
                System.err.println("TrayIcon could not be added.");
            }
        }
    }
    
    public void showNotification(String title, String message, NotificationType type) {
        if (SystemTray.isSupported() && trayIcon != null) {
            MessageType messageType = switch (type) {
                case SUCCESS -> MessageType.INFO;
                case WARNING -> MessageType.WARNING;
                case ERROR -> MessageType.ERROR;
                default -> MessageType.NONE;
            };
            
            trayIcon.displayMessage(title, message, messageType);
        }
        
        // Fallback to JavaFX notification
        Platform.runLater(() -> showJavaFXNotification(title, message, type));
    }
    
    private void showJavaFXNotification(String title, String message, NotificationType type) {
        Popup popup = new Popup();
        
        StackPane content = new StackPane();
        content.setStyle(getNotificationStyle(type));
        content.setPrefSize(300, 80);
        
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label messageLabel = new Label(message);
        messageLabel.setStyle("-fx-font-size: 12px;");
        messageLabel.setWrapText(true);
        
        content.getChildren().addAll(titleLabel, messageLabel);
        StackPane.setAlignment(titleLabel, Pos.TOP_CENTER);
        StackPane.setAlignment(messageLabel, Pos.CENTER);
        
        popup.getContent().add(content);
        
        // Show popup
        Stage stage = (Stage) Stage.getWindows().stream().findFirst().orElse(null);
        if (stage != null) {
            popup.show(stage, stage.getX() + stage.getWidth() - 320, stage.getY() + 50);
            
            // Auto hide after 3 seconds
            PauseTransition delay = new PauseTransition(Duration.seconds(3));
            delay.setOnFinished(e -> popup.hide());
            delay.play();
        }
    }
    
    private String getNotificationStyle(NotificationType type) {
        String baseStyle = "-fx-background-color: %s; -fx-background-radius: 10; " +
                          "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);";
        
        return switch (type) {
            case SUCCESS -> String.format(baseStyle, "#4CAF50");
            case WARNING -> String.format(baseStyle, "#FF9800");
            case ERROR -> String.format(baseStyle, "#F44336");
            default -> String.format(baseStyle, "#2196F3");
        };
    }
    
    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR
    }
}
