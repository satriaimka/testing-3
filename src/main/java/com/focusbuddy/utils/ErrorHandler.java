package com.focusbuddy.utils;

import com.focusbuddy.utils.NotificationManager;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class ErrorHandler {
    private static final Logger logger = Logger.getLogger(ErrorHandler.class.getName());
    private static final String LOG_FILE = "focusbuddy_errors.log";
    private static final int MAX_ERROR_CACHE = 50;

    // Error tracking
    private static final ConcurrentLinkedQueue<ErrorInfo> recentErrors = new ConcurrentLinkedQueue<>();
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static boolean isInitialized = false;

    // Initialize logging
    static {
        initializeLogging();
    }

    private static void initializeLogging() {
        try {
            if (!isInitialized) {
                // Setup file handler for logging
                FileHandler fileHandler = new FileHandler(LOG_FILE, true);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
                logger.setLevel(Level.ALL);
                isInitialized = true;

                logger.info("ErrorHandler initialized successfully");
            }
        } catch (IOException e) {
            System.err.println("Failed to initialize error logging: " + e.getMessage());
        }
    }

    /**
     * Handle error with full exception details and user-friendly dialog
     */
    public static void handleError(String title, String message, Exception e) {
        try {
            // Increment error counter
            int currentErrorCount = errorCount.incrementAndGet();

            // Create error info object
            ErrorInfo errorInfo = new ErrorInfo(title, message, e, LocalDateTime.now());

            // Add to recent errors (with size limit)
            addToRecentErrors(errorInfo);

            // Log the error with full details
            logError(errorInfo, currentErrorCount);

            // Show user-friendly error dialog
            Platform.runLater(() -> showErrorDialog(errorInfo));

            // Send notification
            Platform.runLater(() -> {
                try {
                    NotificationManager.getInstance().showNotification(
                            title,
                            "An error occurred. Check the error dialog for details.",
                            NotificationManager.NotificationType.ERROR
                    );
                } catch (Exception notificationError) {
                    System.err.println("Failed to show error notification: " + notificationError.getMessage());
                }
            });

        } catch (Exception handlingError) {
            // Fallback error handling - don't let error handling itself fail
            System.err.println("Critical error in error handler:");
            System.err.println("Original error: " + title + " - " + message);
            if (e != null) {
                e.printStackTrace();
            }
            System.err.println("Error handling error: " + handlingError.getMessage());
            handlingError.printStackTrace();
        }
    }

    /**
     * Handle error with just title and message (no exception)
     */
    public static void handleError(String title, String message) {
        handleError(title, message, null);
    }

    /**
     * Handle warning (non-critical error)
     */
    public static void handleWarning(String title, String message) {
        try {
            logger.log(Level.WARNING, title + ": " + message);

            Platform.runLater(() -> {
                try {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle(title);
                    alert.setHeaderText(null);
                    alert.setContentText(message);

                    // Apply current theme styling
                    styleAlert(alert);

                    // Add animation
                    addAlertAnimation(alert);

                    alert.showAndWait();
                } catch (Exception e) {
                    System.err.println("Failed to show warning dialog: " + e.getMessage());
                }
            });

            Platform.runLater(() -> {
                try {
                    NotificationManager.getInstance().showNotification(
                            title,
                            message,
                            NotificationManager.NotificationType.WARNING
                    );
                } catch (Exception e) {
                    System.err.println("Failed to show warning notification: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("Failed to handle warning: " + e.getMessage());
        }
    }

    /**
     * Show information message
     */
    public static void showInfo(String title, String message) {
        try {
            logger.info(title + ": " + message);

            Platform.runLater(() -> {
                try {
                    NotificationManager.getInstance().showNotification(
                            title,
                            message,
                            NotificationManager.NotificationType.INFO
                    );
                } catch (Exception e) {
                    System.err.println("Failed to show info notification: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            System.err.println("Failed to show info: " + e.getMessage());
        }
    }

    /**
     * Log error details to file and console
     */
    private static void logError(ErrorInfo errorInfo, int errorNumber) {
        try {
            String logMessage = String.format(
                    "[ERROR #%d] %s at %s\nTitle: %s\nMessage: %s\n%s\n%s\n",
                    errorNumber,
                    Thread.currentThread().getName(),
                    errorInfo.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    errorInfo.title,
                    errorInfo.message,
                    errorInfo.exception != null ? getStackTrace(errorInfo.exception) : "No exception details",
                    "-".repeat(80)
            );

            // Log to logger (which includes file handler)
            logger.severe(logMessage);

            // Also log to console for debugging
            System.err.println(logMessage);

        } catch (Exception e) {
            System.err.println("Failed to log error: " + e.getMessage());
        }
    }

    /**
     * Add error to recent errors cache
     */
    private static void addToRecentErrors(ErrorInfo errorInfo) {
        try {
            recentErrors.offer(errorInfo);

            // Remove old errors if cache is too large
            while (recentErrors.size() > MAX_ERROR_CACHE) {
                recentErrors.poll();
            }
        } catch (Exception e) {
            System.err.println("Failed to cache error: " + e.getMessage());
        }
    }

    /**
     * Show enhanced error dialog with details and actions
     */
    private static void showErrorDialog(ErrorInfo errorInfo) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(errorInfo.title);
            alert.setHeaderText(errorInfo.message);

            // Create main content
            VBox mainContent = new VBox(10);

            // Error summary
            Label summaryLabel = new Label("An error occurred while performing this operation.");
            summaryLabel.setStyle("-fx-font-size: 13px;");

            // Timestamp
            Label timestampLabel = new Label("Time: " +
                    errorInfo.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            timestampLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

            mainContent.getChildren().addAll(summaryLabel, timestampLabel);

            // Error ID for reference
            if (errorCount.get() > 0) {
                Label errorIdLabel = new Label("Error ID: #" + errorCount.get());
                errorIdLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: gray; -fx-font-weight: bold;");
                mainContent.getChildren().add(errorIdLabel);
            }

            alert.getDialogPane().setContent(mainContent);

            // Add expandable exception details if available
            if (errorInfo.exception != null) {
                addExpandableContent(alert, errorInfo.exception);
            }

            // Add custom buttons
            alert.getButtonTypes().clear();
            alert.getButtonTypes().addAll(
                    ButtonType.OK,
                    new ButtonType("Copy Details"),
                    new ButtonType("View Log"),
                    new ButtonType("Report Bug")
            );

            // Apply styling
            styleAlert(alert);

            // Add animation
            addAlertAnimation(alert);

            // Handle button actions
            alert.showAndWait().ifPresent(result -> {
                handleAlertResponse(result, errorInfo);
            });

        } catch (Exception e) {
            // Fallback to simple error message
            System.err.println("Failed to show enhanced error dialog: " + e.getMessage());
            showSimpleErrorDialog(errorInfo);
        }
    }

    /**
     * Add expandable content with exception details
     */
    private static void addExpandableContent(Alert alert, Exception exception) {
        try {
            // Create expandable Exception details
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exception.printStackTrace(pw);
            String exceptionText = sw.toString();

            TextArea textArea = new TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            textArea.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px;");

            // Create a more informative expandable section
            VBox expandableContent = new VBox(8);

            Label detailsLabel = new Label("Technical Details:");
            detailsLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");

            // Exception class and message
            Label exceptionInfo = new Label(
                    "Exception: " + exception.getClass().getSimpleName() + "\n" +
                            "Message: " + (exception.getMessage() != null ? exception.getMessage() : "No message")
            );
            exceptionInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: gray;");

            expandableContent.getChildren().addAll(detailsLabel, exceptionInfo, textArea);

            GridPane.setVgrow(textArea, Priority.ALWAYS);
            GridPane.setHgrow(textArea, Priority.ALWAYS);

            GridPane expContent = new GridPane();
            expContent.setMaxWidth(Double.MAX_VALUE);
            expContent.add(expandableContent, 0, 0);

            alert.getDialogPane().setExpandableContent(expContent);

        } catch (Exception e) {
            System.err.println("Failed to add expandable content: " + e.getMessage());
        }
    }

    /**
     * Style alert with current theme
     */
    private static void styleAlert(Alert alert) {
        try {
            // Try to apply current theme stylesheets
            if (alert.getDialogPane().getScene() != null) {
                // Get stylesheets from a current scene if available
                try {
                    if (ThemeManager.getInstance().getCurrentTheme() != null) {
                        alert.getDialogPane().getStylesheets().clear();
                        String cssPath = "/css/" + ThemeManager.getInstance().getCurrentTheme().getCssFile();
                        alert.getDialogPane().getStylesheets().add(
                                ErrorHandler.class.getResource(cssPath).toExternalForm()
                        );
                    }
                } catch (Exception e) {
                    // Ignore styling errors
                }
            }
        } catch (Exception e) {
            // Ignore styling errors
        }
    }

    /**
     * Add entrance animation to alert
     */
    private static void addAlertAnimation(Alert alert) {
        try {
            Platform.runLater(() -> {
                try {
                    if (alert.getDialogPane() != null) {
                        alert.getDialogPane().setOpacity(0);
                        alert.getDialogPane().setScaleX(0.8);
                        alert.getDialogPane().setScaleY(0.8);

                        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), alert.getDialogPane());
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);

                        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), alert.getDialogPane());
                        scaleIn.setFromX(0.8);
                        scaleIn.setFromY(0.8);
                        scaleIn.setToX(1.0);
                        scaleIn.setToY(1.0);

                        fadeIn.play();
                        scaleIn.play();
                    }
                } catch (Exception e) {
                    // Ignore animation errors
                }
            });
        } catch (Exception e) {
            // Ignore animation errors
        }
    }

    /**
     * Handle alert button responses
     */
    private static void handleAlertResponse(ButtonType result, ErrorInfo errorInfo) {
        try {
            if (result.getText().equals("Copy Details")) {
                copyErrorToClipboard(errorInfo);
            } else if (result.getText().equals("View Log")) {
                openLogFile();
            } else if (result.getText().equals("Report Bug")) {
                showBugReportDialog(errorInfo);
            }
        } catch (Exception e) {
            System.err.println("Failed to handle alert response: " + e.getMessage());
        }
    }

    /**
     * Copy error details to clipboard
     */
    private static void copyErrorToClipboard(ErrorInfo errorInfo) {
        try {
            StringBuilder details = new StringBuilder();
            details.append("FocusBuddy Error Report\n");
            details.append("========================\n");
            details.append("Title: ").append(errorInfo.title).append("\n");
            details.append("Message: ").append(errorInfo.message).append("\n");
            details.append("Time: ").append(errorInfo.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            details.append("Error ID: #").append(errorCount.get()).append("\n\n");

            if (errorInfo.exception != null) {
                details.append("Exception Details:\n");
                details.append(getStackTrace(errorInfo.exception));
            }

            // Copy to clipboard
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(details.toString());
            clipboard.setContent(content);

            showInfo("Copied", "Error details copied to clipboard");

        } catch (Exception e) {
            System.err.println("Failed to copy error to clipboard: " + e.getMessage());
        }
    }

    /**
     * Open log file in default application
     */
    private static void openLogFile() {
        try {
            java.awt.Desktop.getDesktop().open(new java.io.File(LOG_FILE));
        } catch (Exception e) {
            showInfo("Log File", "Log file location: " + new java.io.File(LOG_FILE).getAbsolutePath());
        }
    }

    /**
     * Show bug report dialog
     */
    private static void showBugReportDialog(ErrorInfo errorInfo) {
        try {
            Alert reportAlert = new Alert(Alert.AlertType.INFORMATION);
            reportAlert.setTitle("Report Bug");
            reportAlert.setHeaderText("Help us improve FocusBuddy");

            VBox content = new VBox(10);
            content.getChildren().addAll(
                    new Label("Thank you for helping us improve FocusBuddy!"),
                    new Label("You can report this bug by:"),
                    new Label("• Copying the error details (Copy Details button)"),
                    new Label("• Emailing them to support@focusbuddy.com"),
                    new Label("• Including steps to reproduce the error"),
                    new Label(""),
                    new Label("Error ID: #" + errorCount.get())
            );

            reportAlert.getDialogPane().setContent(content);
            styleAlert(reportAlert);
            reportAlert.showAndWait();

        } catch (Exception e) {
            System.err.println("Failed to show bug report dialog: " + e.getMessage());
        }
    }

    /**
     * Fallback simple error dialog
     */
    private static void showSimpleErrorDialog(ErrorInfo errorInfo) {
        try {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(errorInfo.title);
            alert.setHeaderText(null);
            alert.setContentText(errorInfo.message);
            alert.showAndWait();
        } catch (Exception e) {
            System.err.println("Even simple error dialog failed: " + e.getMessage());
        }
    }

    /**
     * Get stack trace as string
     */
    private static String getStackTrace(Exception e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            return sw.toString();
        } catch (Exception ex) {
            return "Failed to get stack trace: " + ex.getMessage();
        }
    }

    /**
     * Get recent errors for debugging
     */
    public static java.util.List<ErrorInfo> getRecentErrors() {
        return new java.util.ArrayList<>(recentErrors);
    }

    /**
     * Get total error count
     */
    public static int getErrorCount() {
        return errorCount.get();
    }

    /**
     * Clear error cache
     */
    public static void clearErrorCache() {
        recentErrors.clear();
        logger.info("Error cache cleared");
    }

    /**
     * Check if there are recent critical errors
     */
    public static boolean hasRecentCriticalErrors() {
        return recentErrors.stream()
                .anyMatch(error -> error.timestamp.isAfter(LocalDateTime.now().minusMinutes(5)));
    }

    /**
     * Export error log
     */
    public static void exportErrorLog(String filepath) {
        try (FileWriter writer = new FileWriter(filepath)) {
            writer.write("FocusBuddy Error Export\n");
            writer.write("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
            writer.write("Total Errors: " + errorCount.get() + "\n\n");

            for (ErrorInfo error : recentErrors) {
                writer.write("==========================================\n");
                writer.write("Title: " + error.title + "\n");
                writer.write("Message: " + error.message + "\n");
                writer.write("Time: " + error.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                if (error.exception != null) {
                    writer.write("Exception: " + getStackTrace(error.exception) + "\n");
                }
                writer.write("\n");
            }

            showInfo("Export Complete", "Error log exported to: " + filepath);

        } catch (IOException e) {
            handleError("Export Failed", "Failed to export error log", e);
        }
    }

    /**
     * Error information container
     */
    public static class ErrorInfo {
        public final String title;
        public final String message;
        public final Exception exception;
        public final LocalDateTime timestamp;

        public ErrorInfo(String title, String message, Exception exception, LocalDateTime timestamp) {
            this.title = title;
            this.message = message;
            this.exception = exception;
            this.timestamp = timestamp;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s: %s",
                    timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    title, message);
        }
    }
}