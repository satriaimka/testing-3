package com.focusbuddy.controllers;

import com.focusbuddy.database.DatabaseManager;
import com.focusbuddy.models.Mahasiswa;
import com.focusbuddy.utils.ThemeManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.ErrorHandler;
import com.focusbuddy.utils.NotificationManager;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.util.Duration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.focusbuddy.utils.PasswordUtils;
import com.focusbuddy.utils.ValidationUtils;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private VBox loginContainer;
    @FXML private ToggleButton themeToggle;

    private Timeline statusClearTimeline;

    @FXML
    private void initialize() {
        try {
            setupThemeToggle();
            setupEventHandlers();
            setupAnimations();
            setupValidation();

            // Focus on username field when form loads
            Platform.runLater(() -> {
                if (usernameField != null) {
                    usernameField.requestFocus();
                }
            });

            System.out.println("✅ Login controller initialized successfully");
        } catch (Exception e) {
            ErrorHandler.handleError("Login Initialization",
                    "Failed to initialize login screen", e);
        }
    }

    private void setupThemeToggle() {
        try {
            if (themeToggle != null) {
                ThemeManager themeManager = ThemeManager.getInstance();
                themeToggle.setText(themeManager.getCurrentThemeIcon());
                themeToggle.setSelected(themeManager.isDarkTheme());

                themeToggle.setOnAction(e -> toggleThemeWithAnimation());

                // Add hover effect
                addButtonHoverEffect(themeToggle);

                System.out.println("✅ Theme toggle setup completed");
            }
        } catch (Exception e) {
            ErrorHandler.handleError("Theme Setup", "Failed to setup theme toggle", e);
        }
    }

    private void toggleThemeWithAnimation() {
        try {
            Scene scene = loginContainer.getScene();
            ThemeManager themeManager = ThemeManager.getInstance();

            // Create smooth transition
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), loginContainer);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.85);

            fadeOut.setOnFinished(e -> {
                try {
                    themeManager.applyThemeWithAnimation(scene, themeManager.getOppositeTheme());
                    themeToggle.setText(themeManager.getCurrentThemeIcon());

                    FadeTransition fadeIn = new FadeTransition(Duration.millis(150), loginContainer);
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
            ThemeManager themeManager = ThemeManager.getInstance();
            themeManager.toggleTheme(loginContainer.getScene());
            if (themeToggle != null) {
                themeToggle.setText(themeManager.getCurrentThemeIcon());
            }
        }
    }

    private void setupEventHandlers() {
        try {
            // Button actions with error handling
            if (loginButton != null) {
                loginButton.setOnAction(e -> handleLoginWithAnimation());
                addButtonHoverEffect(loginButton);
            }

            if (registerButton != null) {
                registerButton.setOnAction(e -> showRegisterDialog());
                addButtonHoverEffect(registerButton);
            }

            // Enter key handlers
            if (passwordField != null) {
                passwordField.setOnAction(e -> handleLoginWithAnimation());
            }

            if (usernameField != null) {
                usernameField.setOnAction(e -> {
                    if (passwordField != null) {
                        passwordField.requestFocus();
                    }
                });
            }

            System.out.println("✅ Event handlers setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Event Setup", "Failed to setup event handlers", e);
        }
    }

    private void addButtonHoverEffect(Control control) {
        try {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), control);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), control);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            control.setOnMouseEntered(e -> scaleUp.play());
            control.setOnMouseExited(e -> scaleDown.play());
        } catch (Exception e) {
            System.err.println("Warning: Could not add hover effect to control: " + e.getMessage());
        }
    }

    private void setupAnimations() {
        try {
            if (loginContainer != null) {
                // Entrance animation for the login container
                loginContainer.setOpacity(0);
                loginContainer.setTranslateY(30);

                Platform.runLater(() -> {
                    try {
                        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), loginContainer);
                        fadeIn.setFromValue(0);
                        fadeIn.setToValue(1);

                        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.8), loginContainer);
                        slideIn.setFromY(30);
                        slideIn.setToY(0);
                        slideIn.setInterpolator(Interpolator.EASE_OUT);

                        ParallelTransition entrance = new ParallelTransition(fadeIn, slideIn);
                        entrance.play();
                    } catch (Exception e) {
                        // Fallback: just show the container
                        loginContainer.setOpacity(1);
                        loginContainer.setTranslateY(0);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not setup entrance animations: " + e.getMessage());
        }
    }

    private void setupValidation() {
        try {
            // Real-time validation feedback
            if (usernameField != null) {
                usernameField.textProperty().addListener((obs, oldText, newText) -> {
                    if (!newText.trim().isEmpty() && !ValidationUtils.isValidUsername(newText)) {
                        usernameField.setStyle(usernameField.getStyle() + " -fx-border-color: #ef4444;");
                    } else {
                        usernameField.setStyle(usernameField.getStyle().replace(" -fx-border-color: #ef4444;", ""));
                    }
                });
            }

            if (passwordField != null) {
                passwordField.textProperty().addListener((obs, oldText, newText) -> {
                    if (!newText.isEmpty() && !ValidationUtils.isValidPassword(newText)) {
                        passwordField.setStyle(passwordField.getStyle() + " -fx-border-color: #ef4444;");
                    } else {
                        passwordField.setStyle(passwordField.getStyle().replace(" -fx-border-color: #ef4444;", ""));
                    }
                });
            }

            System.out.println("✅ Validation setup completed");
        } catch (Exception e) {
            System.err.println("Warning: Could not setup validation: " + e.getMessage());
        }
    }

    private void handleLoginWithAnimation() {
        if (loginButton == null) return;

        try {
            // Disable button and show loading state
            loginButton.setDisable(true);
            String originalText = loginButton.getText();
            loginButton.setText("Signing in...");

            // Add loading animation
            RotateTransition rotate = new RotateTransition(Duration.seconds(1), loginButton);
            rotate.setByAngle(360);
            rotate.setCycleCount(Timeline.INDEFINITE);
            rotate.play();

            // Perform actual login in background thread
            Task<Boolean> loginTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    return performLogin();
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        rotate.stop();
                        loginButton.setDisable(false);
                        loginButton.setText(originalText);
                        loginButton.setRotate(0);

                        if (getValue()) {
                            // Login successful - navigate to dashboard
                            showSuccessAnimation();
                        }
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        rotate.stop();
                        loginButton.setDisable(false);
                        loginButton.setText(originalText);
                        loginButton.setRotate(0);

                        Throwable exception = getException();
                        ErrorHandler.handleError("Login Failed",
                                "An error occurred during login",
                                exception instanceof Exception ? (Exception) exception :
                                        new Exception(exception));
                    });
                }
            };

            Thread loginThread = new Thread(loginTask);
            loginThread.setDaemon(true);
            loginThread.start();

        } catch (Exception e) {
            // Reset button state
            if (loginButton != null) {
                loginButton.setDisable(false);
                loginButton.setText("Sign In");
            }
            ErrorHandler.handleError("Login Error", "Failed to start login process", e);
        }
    }

    private boolean performLogin() throws Exception {
        String username = usernameField != null ? usernameField.getText().trim() : "";
        String password = passwordField != null ? passwordField.getText() : "";

        // Clear previous status
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("");
            }
        });

        if (!ValidationUtils.isNotEmpty(username) || !ValidationUtils.isNotEmpty(password)) {
            Platform.runLater(() -> showStatusWithAnimation("Please fill in all fields", false));
            return false;
        }

        if (!ValidationUtils.isValidUsername(username)) {
            Platform.runLater(() -> showStatusWithAnimation("Invalid username format", false));
            return false;
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String query = "SELECT * FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String salt = rs.getString("salt");

                // For backward compatibility, check if salt exists
                boolean passwordValid;
                if (salt != null) {
                    passwordValid = PasswordUtils.verifyPassword(password, storedPassword, salt);
                } else {
                    // Legacy plain text password check
                    passwordValid = password.equals(storedPassword);

                    // Upgrade to hashed password
                    if (passwordValid) {
                        String newSalt = PasswordUtils.generateSalt();
                        String hashedPassword = PasswordUtils.hashPassword(password, newSalt);

                        String updateQuery = "UPDATE users SET password = ?, salt = ? WHERE id = ?";
                        PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, hashedPassword);
                        updateStmt.setString(2, newSalt);
                        updateStmt.setInt(3, rs.getInt("id"));
                        updateStmt.executeUpdate();
                    }
                }

                if (passwordValid) {
                    // Create user object
                    Mahasiswa user = new Mahasiswa();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setFullName(rs.getString("full_name"));

                    // Set current user session
                    UserSession.getInstance().setCurrentUser(user);

                    // ✅ HAPUS LOGIC PEMBERSIHAN DATA - BIARKAN DATA USER TETAP ADA
                    // Data user seharusnya tersimpan dan tidak dihapus setiap login

                    Platform.runLater(() -> {
                        String welcomeMessage = "Welcome back, " + user.getFullName() + "! 🎉";
                        showStatusWithAnimation(welcomeMessage, true);

                        // Show success notification
                        NotificationManager.getInstance().showNotification(
                                "Login Successful",
                                "Welcome to FocusBuddy!",
                                NotificationManager.NotificationType.SUCCESS
                        );
                    });

                    // Navigate to dashboard with delay for user to see success message
                    Platform.runLater(() -> {
                        Timeline delay = new Timeline(new KeyFrame(Duration.seconds(1.5),
                                e -> navigateToMainDashboard()));
                        delay.play();
                    });

                    return true;
                } else {
                    Platform.runLater(() -> {
                        showStatusWithAnimation("Invalid username or password", false);
                        shakeForm();
                    });
                    return false;
                }
            } else {
                Platform.runLater(() -> {
                    showStatusWithAnimation("Invalid username or password", false);
                    shakeForm();
                });
                return false;
            }

        } catch (SQLException e) {
            Platform.runLater(() -> {
                showStatusWithAnimation("Database connection failed. Please try again.", false);
                shakeForm();
            });
            throw new Exception("Database error during login", e);
        }
    }

    private void showSuccessAnimation() {
        try {
            if (loginContainer != null) {
                // Success pulse animation
                ScaleTransition pulse = new ScaleTransition(Duration.millis(200), loginContainer);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.02);
                pulse.setToY(1.02);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        } catch (Exception e) {
            System.err.println("Warning: Could not show success animation: " + e.getMessage());
        }
    }

    private void shakeForm() {
        if (loginContainer == null) return;

        try {
            TranslateTransition shake = new TranslateTransition(Duration.millis(50), loginContainer);
            shake.setFromX(0);
            shake.setByX(10);
            shake.setCycleCount(6);
            shake.setAutoReverse(true);
            shake.setOnFinished(e -> loginContainer.setTranslateX(0));
            shake.play();
        } catch (Exception e) {
            System.err.println("Warning: Could not show shake animation: " + e.getMessage());
        }
    }

    private void showRegisterDialog() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Create Account");
            dialog.setHeaderText("Join FocusBuddy Community");

            // Apply current theme to dialog
            dialog.getDialogPane().getStylesheets().clear();
            if (loginContainer != null && loginContainer.getScene() != null) {
                dialog.getDialogPane().getStylesheets().addAll(
                        loginContainer.getScene().getStylesheets());
            }

            // Create form fields with validation
            TextField regUsername = createValidatedTextField("Choose a username", this::validateUsernameInput);
            PasswordField regPassword = createValidatedPasswordField("Create a strong password");
            PasswordField confirmPassword = createValidatedPasswordField("Confirm your password");
            TextField regEmail = createValidatedTextField("Your email address", this::validateEmailInput);
            TextField regFullName = createValidatedTextField("Your full name", this::validateNameInput);

            VBox content = new VBox(15);
            content.getChildren().addAll(
                    new Label("Username:"), regUsername,
                    new Label("Password:"), regPassword,
                    new Label("Confirm Password:"), confirmPassword,
                    new Label("Email:"), regEmail,
                    new Label("Full Name:"), regFullName
            );
            content.getStyleClass().add("form-container");

            dialog.getDialogPane().setContent(content);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            // Style the buttons
            Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (okButton != null) {
                okButton.getStyleClass().add("primary-button");
                okButton.setText("Create Account");
            }
            if (cancelButton != null) {
                cancelButton.getStyleClass().add("secondary-button");
                cancelButton.setText("Cancel");
            }

            // Add entrance animation to dialog
            Platform.runLater(() -> {
                if (dialog.getDialogPane() != null) {
                    dialog.getDialogPane().setOpacity(0);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), dialog.getDialogPane());
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();
                }
            });

            dialog.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    try {
                        String password = regPassword.getText();
                        String confirmPwd = confirmPassword.getText();

                        if (!password.equals(confirmPwd)) {
                            showStatusWithAnimation("Passwords do not match", false);
                            return;
                        }

                        registerUser(regUsername.getText(), password, regEmail.getText(), regFullName.getText());
                    } catch (Exception e) {
                        ErrorHandler.handleError("Registration", "Failed to process registration", e);
                    }
                }
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Registration Dialog", "Failed to show registration dialog", e);
        }
    }

    private TextField createValidatedTextField(String promptText, ValidationCallback validator) {
        TextField field = new TextField();
        field.setPromptText(promptText);
        field.getStyleClass().add("custom-text-field");

        if (validator != null) {
            field.textProperty().addListener((obs, oldText, newText) -> {
                try {
                    boolean isValid = validator.validate(newText);
                    if (!newText.trim().isEmpty() && !isValid) {
                        field.setStyle(field.getStyle() + " -fx-border-color: #ef4444;");
                    } else {
                        field.setStyle(field.getStyle().replace(" -fx-border-color: #ef4444;", ""));
                    }
                } catch (Exception e) {
                    System.err.println("Validation error: " + e.getMessage());
                }
            });
        }

        return field;
    }

    private PasswordField createValidatedPasswordField(String promptText) {
        PasswordField field = new PasswordField();
        field.setPromptText(promptText);
        field.getStyleClass().add("custom-text-field");

        field.textProperty().addListener((obs, oldText, newText) -> {
            try {
                if (!newText.isEmpty() && !ValidationUtils.isValidPassword(newText)) {
                    field.setStyle(field.getStyle() + " -fx-border-color: #ef4444;");
                } else {
                    field.setStyle(field.getStyle().replace(" -fx-border-color: #ef4444;", ""));
                }
            } catch (Exception e) {
                System.err.println("Password validation error: " + e.getMessage());
            }
        });

        return field;
    }

    @FunctionalInterface
    private interface ValidationCallback {
        boolean validate(String input);
    }

    private boolean validateUsernameInput(String input) {
        return ValidationUtils.isValidUsername(input);
    }

    private boolean validateEmailInput(String input) {
        return input.isEmpty() || ValidationUtils.isValidEmail(input);
    }

    private boolean validateNameInput(String input) {
        return input.trim().length() >= 2;
    }

    private void registerUser(String username, String password, String email, String fullName) {
        try {
            // Validate inputs
            if (!ValidationUtils.isValidUsername(username)) {
                showStatusWithAnimation("Username must be 3-20 characters, letters, numbers, and underscores only", false);
                return;
            }

            if (!ValidationUtils.isValidPassword(password)) {
                showStatusWithAnimation("Password must be at least 6 characters long", false);
                return;
            }

            if (!email.isEmpty() && !ValidationUtils.isValidEmail(email)) {
                showStatusWithAnimation("Please enter a valid email address", false);
                return;
            }

            if (fullName.trim().length() < 2) {
                showStatusWithAnimation("Full name must be at least 2 characters long", false);
                return;
            }

            // Sanitize inputs and create final variables for use in inner class
            final String finalUsername = ValidationUtils.sanitizeInput(username);
            final String finalEmail = ValidationUtils.sanitizeInput(email);
            final String finalFullName = ValidationUtils.sanitizeInput(fullName);
            final String finalPassword = password; // Don't sanitize password

            // Perform registration in background thread
            Task<Boolean> registrationTask = new Task<Boolean>() {
                @Override
                protected Boolean call() throws Exception {
                    try (Connection conn = DatabaseManager.getInstance().getConnection()) {
                        // Check if username already exists
                        String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
                        PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                        checkStmt.setString(1, finalUsername);
                        ResultSet checkRs = checkStmt.executeQuery();

                        if (checkRs.next() && checkRs.getInt(1) > 0) {
                            Platform.runLater(() ->
                                    showStatusWithAnimation("Username already exists. Please choose a different username.", false));
                            return false;
                        }

                        // Generate salt and hash password
                        String salt = PasswordUtils.generateSalt();
                        String hashedPassword = PasswordUtils.hashPassword(finalPassword, salt);

                        String query = "INSERT INTO users (username, password, salt, email, full_name) VALUES (?, ?, ?, ?, ?)";
                        PreparedStatement stmt = conn.prepareStatement(query);
                        stmt.setString(1, finalUsername);
                        stmt.setString(2, hashedPassword);
                        stmt.setString(3, salt);
                        stmt.setString(4, finalEmail);
                        stmt.setString(5, finalFullName);

                        int result = stmt.executeUpdate();
                        return result > 0;
                    }
                }

                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        if (getValue()) {
                            showStatusWithAnimation("Account created successfully! Please sign in with your new account. ✨", true);

                            // Pre-fill username for convenience
                            if (usernameField != null) {
                                usernameField.setText(finalUsername);
                            }
                            if (passwordField != null) {
                                passwordField.requestFocus();
                            }

                            // Show success notification
                            NotificationManager.getInstance().showNotification(
                                    "Registration Successful",
                                    "Welcome to FocusBuddy! You can now sign in.",
                                    NotificationManager.NotificationType.SUCCESS
                            );
                        }
                    });
                }

                @Override
                protected void failed() {
                    Platform.runLater(() -> {
                        Throwable exception = getException();
                        String message = exception.getMessage();

                        if (message != null && message.contains("Duplicate entry")) {
                            showStatusWithAnimation("Username already exists. Please choose a different username.", false);
                        } else {
                            showStatusWithAnimation("Registration failed. Please try again.", false);
                        }

                        ErrorHandler.handleError("Registration Failed",
                                "User registration failed",
                                exception instanceof Exception ? (Exception) exception :
                                        new Exception(exception));
                    });
                }
            };

            Thread registrationThread = new Thread(registrationTask);
            registrationThread.setDaemon(true);
            registrationThread.start();

        } catch (Exception e) {
            ErrorHandler.handleError("Registration Error", "Failed to start registration process", e);
        }
    }

    private void navigateToMainDashboard() {
        try {
            // Get current stage
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // Create fade out animation
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(0.6), loginContainer.getScene().getRoot());
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                try {
                    // Load dashboard
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/dashboard.fxml"));
                    Scene dashboardScene = new Scene(loader.load());

                    // Apply current theme
                    ThemeManager.getInstance().applyTheme(dashboardScene,
                            ThemeManager.getInstance().getCurrentTheme());

                    // Get screen dimensions
                    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

                    // Configure window for dashboard - MAXIMIZE TO FULL SCREEN
                    currentStage.setScene(dashboardScene);
                    currentStage.setTitle("FocusBuddy - Dashboard");

                    // CRITICAL: Set window to be resizable FIRST
                    currentStage.setResizable(true);

                    // Set minimum size constraints
                    currentStage.setMinWidth(1200);
                    currentStage.setMinHeight(800);

                    // MAXIMIZE the window to fill the screen
                    currentStage.setMaximized(true);

                    // If maximized doesn't work, set to full screen bounds
                    if (!currentStage.isMaximized()) {
                        currentStage.setX(0);
                        currentStage.setY(0);
                        currentStage.setWidth(screenBounds.getWidth());
                        currentStage.setHeight(screenBounds.getHeight());
                    }

                    // Ensure window is properly shown and focused
                    currentStage.show();
                    currentStage.toFront();
                    currentStage.requestFocus();

                    // Smooth fade in for dashboard
                    dashboardScene.getRoot().setOpacity(0);
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), dashboardScene.getRoot());
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();

                } catch (Exception ex) {
                    ErrorHandler.handleError("Navigation Error",
                            "Failed to load dashboard", ex);
                }
            });

            fadeOut.play();

        } catch (Exception e) {
            ErrorHandler.handleError("Navigation Error",
                    "Failed to navigate to dashboard", e);
        }
    }

    private void showStatusWithAnimation(String message, boolean isSuccess) {
        if (statusLabel == null) return;

        try {
            // Clear any existing timeline
            if (statusClearTimeline != null) {
                statusClearTimeline.stop();
            }

            statusLabel.setText(message);
            statusLabel.setStyle(isSuccess ?
                    "-fx-text-fill: #10b981; -fx-font-weight: bold;" :
                    "-fx-text-fill: #ef4444; -fx-font-weight: bold;");

            // Fade in animation for status message
            statusLabel.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), statusLabel);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();

            // Auto-clear status after some time (only for error messages)
            if (!isSuccess) {
                statusClearTimeline = new Timeline(new KeyFrame(Duration.seconds(5),
                        e -> {
                            try {
                                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), statusLabel);
                                fadeOut.setFromValue(1);
                                fadeOut.setToValue(0);
                                fadeOut.setOnFinished(event -> statusLabel.setText(""));
                                fadeOut.play();
                            } catch (Exception ex) {
                                statusLabel.setText("");
                            }
                        }));
                statusClearTimeline.play();
            }
        } catch (Exception e) {
            // Fallback: just set the text without animation
            statusLabel.setText(message);
            statusLabel.setOpacity(1);
        }
    }
}