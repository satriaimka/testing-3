package com.focusbuddy.controllers;

import com.focusbuddy.models.RegularUser;
import com.focusbuddy.models.User;
import com.focusbuddy.services.UserService;
import com.focusbuddy.utils.ErrorHandler;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;


import java.util.concurrent.CompletableFuture;

/**
 * STEP 4: Buat file ProfileDialogController.java di package com.focusbuddy.controllers
 * Controller ini menangani dialog profil user
 */
public class ProfileDialogController {

    // FXML Elements - sesuai dengan fx:id di file FXML
    @FXML private VBox profileContainer;
    @FXML private Circle profileAvatarCircle;
    @FXML private Label userInitialsLabel;
    @FXML private Label userFullNameLabel;
    @FXML private Label userUsernameLabel;
    @FXML private Label userJoinDateLabel;

    // Labels untuk statistik
    @FXML private Label tasksStatLabel;
    @FXML private Label focusTimeStatLabel;
    @FXML private Label goalsStatLabel;

    // Fields untuk edit profil
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private Button saveProfileBtn;

    // Fields untuk ganti password
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button changePasswordBtn;

    // Tombol dan label
    @FXML private Button closeBtn;
    @FXML private Label statusLabel;

    // Services dan variables
    private UserService userService;
    private User currentUser;
    private Stage dialogStage;

    /**
     * Method yang dipanggil otomatis saat FXML dimuat
     */
    @FXML
    private void initialize() {
        try {
            // Initialize service dan user
            userService = new UserService();
            currentUser = UserSession.getInstance().getCurrentUser();

            if (currentUser == null) {
                showStatus("No user session found", false);
                return;
            }

            // Setup event handlers untuk tombol
            setupEventHandlers();

            // Load data user
            loadUserData();

            // Load statistik user
            loadUserStats();

        } catch (Exception e) {
            ErrorHandler.handleError("Profile Initialization", "Failed to initialize profile dialog", e);
        }
    }

    /**
     * Set dialog stage (dipanggil dari DashboardController)
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        setupWindowResizeHandler(dialogStage);
    }

    /**
     * Setup event handlers untuk semua tombol
     */
    private void setupEventHandlers() {
        // Tombol Save Profile
        if (saveProfileBtn != null) {
            saveProfileBtn.setOnAction(e -> handleSaveProfile());
        }

        // Tombol Change Password
        if (changePasswordBtn != null) {
            changePasswordBtn.setOnAction(e -> handleChangePassword());
        }

        // Tombol Close
        if (closeBtn != null) {
            closeBtn.setOnAction(e -> handleClose());
        }

        // Setup validasi real-time
        setupFieldValidation();
    }

    /**
     * Setup validasi untuk enable/disable tombol
     */
    private void setupFieldValidation() {
        // Enable/disable tombol save profile berdasarkan perubahan field
        if (fullNameField != null && emailField != null && saveProfileBtn != null) {
            fullNameField.textProperty().addListener((obs, oldVal, newVal) -> updateSaveButtonState());
            emailField.textProperty().addListener((obs, oldVal, newVal) -> updateSaveButtonState());
        }

        // Enable/disable tombol change password berdasarkan kelengkapan field
        if (currentPasswordField != null && newPasswordField != null &&
                confirmPasswordField != null && changePasswordBtn != null) {

            currentPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordButtonState());
            newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordButtonState());
            confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> updatePasswordButtonState());
        }
    }

    /**
     * Update state tombol save profile
     */
    private void updateSaveButtonState() {
        if (saveProfileBtn == null || fullNameField == null || emailField == null) return;

        // Cek apakah ada perubahan
        boolean hasChanges = !fullNameField.getText().equals(currentUser.getFullName() != null ? currentUser.getFullName() : "") ||
                !emailField.getText().equals(currentUser.getEmail() != null ? currentUser.getEmail() : "");

        // Cek apakah data valid
        boolean isValid = ValidationUtils.isNotEmpty(fullNameField.getText()) &&
                ValidationUtils.isValidEmail(emailField.getText());

        saveProfileBtn.setDisable(!hasChanges || !isValid);
    }

    /**
     * Update state tombol change password
     */
    private void updatePasswordButtonState() {
        if (changePasswordBtn == null) return;

        boolean allFieldsFilled = ValidationUtils.isNotEmpty(currentPasswordField.getText()) &&
                ValidationUtils.isNotEmpty(newPasswordField.getText()) &&
                ValidationUtils.isNotEmpty(confirmPasswordField.getText());

        changePasswordBtn.setDisable(!allFieldsFilled);
    }

    /**
     * Load data user ke tampilan
     */
    private void loadUserData() {
        try {
            if (currentUser == null) return;

            // Load info dasar
            if (userInitialsLabel != null) {
                String initials = currentUser instanceof RegularUser ?
                        ((RegularUser) currentUser).getInitials() :
                        currentUser.getUsername().substring(0, Math.min(2, currentUser.getUsername().length())).toUpperCase();
                userInitialsLabel.setText(initials);
            }

            if (userFullNameLabel != null) {
                userFullNameLabel.setText(currentUser.getFullName() != null ?
                        currentUser.getFullName() : currentUser.getUsername());
            }

            if (userUsernameLabel != null) {
                userUsernameLabel.setText("@" + currentUser.getUsername());
            }

            if (userJoinDateLabel != null) {
                String joinDate = currentUser instanceof RegularUser ?
                        ((RegularUser) currentUser).getFormattedJoinDate() : "Member";
                userJoinDateLabel.setText(joinDate);
            }

            // Load form fields
            if (fullNameField != null) {
                fullNameField.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
            }

            if (emailField != null) {
                emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
            }

            // Update button states
            updateSaveButtonState();
            updatePasswordButtonState();

        } catch (Exception e) {
            showStatus("Failed to load user data: " + e.getMessage(), false);
        }
    }

    /**
     * Load statistik user
     */
    private void loadUserStats() {
        // Load statistik secara asynchronous
        CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getUserStats(currentUser.getId());
            } catch (Exception e) {
                System.err.println("Error loading user stats: " + e.getMessage());
                return new UserService.UserStats(); // Return empty stats
            }
        }).thenAccept(stats -> {
            Platform.runLater(() -> {
                updateStatsDisplay(stats);
            });
        });
    }

    /**
     * Update tampilan statistik
     */
    private void updateStatsDisplay(UserService.UserStats stats) {
        try {
            if (tasksStatLabel != null) {
                tasksStatLabel.setText(String.valueOf(stats.completedTasks));
            }

            if (focusTimeStatLabel != null) {
                focusTimeStatLabel.setText(stats.getFormattedFocusTime());
            }

            if (goalsStatLabel != null) {
                if (stats.totalGoals > 0) {
                    goalsStatLabel.setText(stats.completedGoals + "/" + stats.totalGoals);
                } else {
                    goalsStatLabel.setText("0");
                }
            }

        } catch (Exception e) {
            System.err.println("Error updating stats display: " + e.getMessage());
        }
    }

    /**
     * Handle tombol Save Profile
     */
    private void handleSaveProfile() {
        try {
            String newFullName = fullNameField.getText().trim();
            String newEmail = emailField.getText().trim();

            // Validasi input
            if (!ValidationUtils.isNotEmpty(newFullName)) {
                showStatus("Full name cannot be empty", false);
                return;
            }

            if (!ValidationUtils.isValidEmail(newEmail)) {
                showStatus("Please enter a valid email address", false);
            }

            // Show loading
            showStatus("Saving profile...", true);
            saveProfileBtn.setDisable(true);

            // Save secara asynchronous
            CompletableFuture.supplyAsync(() -> {
                try {
                    return userService.updateUserProfile(currentUser, newFullName, newEmail);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(success -> {
                Platform.runLater(() -> {
                    if (success) {
                        showStatus("Profile updated successfully! ✓", true);

                        // Update tampilan
                        userFullNameLabel.setText(currentUser.getFullName());

                        // Show notification
                        NotificationManager.getInstance().showNotification(
                                "Profile Updated",
                                "Your profile has been updated successfully! 🎉",
                                NotificationManager.NotificationType.SUCCESS
                        );

                        updateSaveButtonState();
                    } else {
                        showStatus("Failed to update profile", false);
                    }
                    saveProfileBtn.setDisable(false);
                });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showStatus("Error: " + throwable.getCause().getMessage(), false);
                    saveProfileBtn.setDisable(false);
                });
                return null;
            });

        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), false);
            saveProfileBtn.setDisable(false);
        }
    }

    /**
     * Handle tombol Change Password
     */
    private void handleChangePassword() {
        try {
            String currentPassword = currentPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Validasi input
            if (!ValidationUtils.isValidPassword(newPassword)) {
                showStatus("New password must be at least 6 characters long", false);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showStatus("New passwords do not match", false);
                return;
            }

            // Show loading
            showStatus("Changing password...", true);
            changePasswordBtn.setDisable(true);

            // Change password secara asynchronous
            CompletableFuture.supplyAsync(() -> {
                try {
                    return userService.changePassword(currentUser, currentPassword, newPassword);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).thenAccept(success -> {
                Platform.runLater(() -> {
                    if (success) {
                        showStatus("Password changed successfully! ✓", true);

                        // Clear password fields
                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();

                        // Show notification
                        NotificationManager.getInstance().showNotification(
                                "Password Changed",
                                "Your password has been changed successfully! 🔒",
                                NotificationManager.NotificationType.SUCCESS
                        );

                        updatePasswordButtonState();
                    } else {
                        showStatus("Failed to change password", false);
                    }
                    changePasswordBtn.setDisable(false);
                });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showStatus("Error: " + throwable.getCause().getMessage(), false);
                    changePasswordBtn.setDisable(false);
                });
                return null;
            });

        } catch (Exception e) {
            showStatus("Error: " + e.getMessage(), false);
            changePasswordBtn.setDisable(false);
        }
    }

    /**
     * Handle tombol Close
     */
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Show status message
     */
    private void showStatus(String message, boolean isSuccess) {
        try {
            if (statusLabel != null) {
                statusLabel.setText(message);
                statusLabel.setVisible(true);

                // Apply style
                statusLabel.getStyleClass().removeAll("status-success", "status-error");
                statusLabel.getStyleClass().add(isSuccess ? "status-success" : "status-error");

                // Auto-hide untuk pesan sukses
                if (isSuccess) {
                    Platform.runLater(() -> {
                        try {
                            Thread.sleep(3000);
                            Platform.runLater(() -> {
                                if (statusLabel != null) {
                                    statusLabel.setVisible(false);
                                }
                            });
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Error showing status: " + e.getMessage());
        }
    }

    public void setupResponsiveDialog(Stage dialogStage) {
        try {
            // Get screen dimensions
            Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
            double screenHeight = screenBounds.getHeight();
            double screenWidth = screenBounds.getWidth();

            // Calculate dialog size based on screen size
            double dialogHeight = Math.min(580, screenHeight * 0.85); // Max 85% of screen height
            double dialogWidth = Math.min(460, screenWidth * 0.9);    // Max 90% of screen width

            // Set dialog size
            dialogStage.setWidth(dialogWidth);
            dialogStage.setHeight(dialogHeight);

            // Update container size
            if (profileContainer != null) {
                profileContainer.setPrefHeight(dialogHeight - 40); // Account for window decorations
                profileContainer.setMaxHeight(dialogHeight - 40);
                profileContainer.setPrefWidth(dialogWidth - 20);
                profileContainer.setMaxWidth(dialogWidth - 20);
            }

            System.out.println("✅ Dialog size adjusted: " + dialogWidth + "x" + dialogHeight);

        } catch (Exception e) {
            System.err.println("Error setting up responsive dialog: " + e.getMessage());
        }
    }

    private void setupWindowResizeHandler(Stage dialogStage) {
        try {
            if (dialogStage != null && profileContainer != null) {
                dialogStage.widthProperty().addListener((obs, oldWidth, newWidth) -> {
                    Platform.runLater(() -> {
                        profileContainer.setPrefWidth(newWidth.doubleValue() - 20);
                    });
                });

                dialogStage.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                    Platform.runLater(() -> {
                        profileContainer.setPrefHeight(newHeight.doubleValue() - 40);
                    });
                });
            }
        } catch (Exception e) {
            System.err.println("Error setting up window resize handler: " + e.getMessage());
        }
    }
}