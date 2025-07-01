package com.focusbuddy.controllers;

import com.focusbuddy.models.Task;
import com.focusbuddy.services.TaskService;
import com.focusbuddy.utils.NotificationManager;
import com.focusbuddy.utils.UserSession;
import com.focusbuddy.utils.ErrorHandler;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
// Remove the ambiguous import and use fully qualified name when needed
// import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.application.Platform;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class TasksController {

    @FXML private VBox tasksContainer;
    @FXML private ListView<Task> tasksList;
    @FXML private TextField taskTitleField;
    @FXML private TextArea taskDescriptionArea;
    @FXML private ComboBox<Task.Priority> priorityCombo;
    @FXML private ComboBox<Task.Status> statusCombo;
    @FXML private DatePicker dueDatePicker;
    @FXML private Button saveTaskButton;
    @FXML private Button newTaskButton;
    @FXML private Button deleteTaskButton;

    // Filtering and sorting controls
    @FXML private TextField searchField;
    @FXML private ComboBox<Task.Priority> filterPriorityCombo;
    @FXML private ComboBox<Task.Status> filterStatusCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private CheckBox showCompletedCheckBox;

    // Statistics
    @FXML private Label totalTasksLabel;
    @FXML private Label pendingTasksLabel;
    @FXML private Label completedTasksLabel;
    @FXML private Label overdueTasksLabel;

    private TaskService taskService;
    private Task currentTask;
    private ObservableList<Task> allTasks;
    private Timeline autoSaveTimeline;

    @FXML
    private void initialize() {
        try {
            taskService = new TaskService();
            allTasks = FXCollections.observableArrayList();

            setupTasksList();
            setupComboBoxes();
            setupButtons();
            setupFilters();
            setupAutoSave();
            setupKeyboardShortcuts();

            // ✅ UBAH: Jangan clear, load data real
            loadTasksSafely();
            updateStatistics();

            addEntranceAnimation();

            System.out.println("✅ Tasks controller initialized successfully");
        } catch (Exception e) {
            ErrorHandler.handleError("Tasks Initialization",
                    "Failed to initialize tasks view", e);
        }
    }

    private void setupTasksList() {
        try {
            tasksList.setCellFactory(listView -> new ListCell<Task>() {
                @Override
                protected void updateItem(Task task, boolean empty) {
                    super.updateItem(task, empty);
                    if (empty || task == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("");
                    } else {
                        setGraphic(createTaskListItem(task));
                        setStyle(getTaskItemStyle(task));

                        // Add hover effect
                        setOnMouseEntered(e -> {
                            if (!isEmpty()) {
                                ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                                scale.setToX(1.02);
                                scale.setToY(1.02);
                                scale.play();
                            }
                        });

                        setOnMouseExited(e -> {
                            if (!isEmpty()) {
                                ScaleTransition scale = new ScaleTransition(Duration.millis(100), this);
                                scale.setToX(1.0);
                                scale.setToY(1.0);
                                scale.play();
                            }
                        });
                    }
                }
            });

            tasksList.getSelectionModel().selectedItemProperty().addListener((obs, oldTask, newTask) -> {
                if (newTask != null) {
                    loadTaskDetails(newTask);
                }
            });

            System.out.println("✅ Tasks list setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Tasks List Setup", "Failed to setup tasks list", e);
        }
    }

    private VBox createTaskListItem(Task task) {
        try {
            VBox content = new VBox(5);
            content.setMaxWidth(Double.MAX_VALUE);

            // Title with priority indicator
            HBox titleRow = new HBox(8);
            titleRow.setMaxWidth(Double.MAX_VALUE);

            Label priorityIndicator = new Label(getPriorityIndicator(task.getPriority()));
            priorityIndicator.setStyle("-fx-font-size: 12px;");

            Label titleLabel = new Label(task.getTitle());
            titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);

            Label statusLabel = new Label(task.getStatus().toString());
            statusLabel.setStyle(getStatusStyle(task.getStatus()) + " -fx-font-size: 10px; -fx-padding: 2 6 2 6; -fx-background-radius: 8;");

            titleRow.getChildren().addAll(priorityIndicator, titleLabel, statusLabel);

            // Description
            if (task.getDescription() != null && !task.getDescription().trim().isEmpty()) {
                Label descLabel = new Label(task.getDescription());
                descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
                descLabel.setWrapText(true);
                descLabel.setMaxWidth(Double.MAX_VALUE);
                content.getChildren().add(descLabel);
            }

            // Due date and progress info
            HBox bottomRow = new HBox(10);
            bottomRow.setMaxWidth(Double.MAX_VALUE);

            if (task.getDueDate() != null) {
                Label dueDateLabel = new Label("📅 " + formatDueDate(task.getDueDate()));
                dueDateLabel.setStyle("-fx-font-size: 11px;");

                // Color coding for due dates
                if (task.getDueDate().isBefore(LocalDate.now()) &&
                        task.getStatus() != Task.Status.COMPLETED) {
                    dueDateLabel.setStyle(dueDateLabel.getStyle() + " -fx-text-fill: #ef4444;");
                } else if (task.getDueDate().equals(LocalDate.now())) {
                    dueDateLabel.setStyle(dueDateLabel.getStyle() + " -fx-text-fill: #f59e0b;");
                }

                bottomRow.getChildren().add(dueDateLabel);
            }

            // Quick action buttons
            HBox quickActions = new HBox(5);

            Button completeBtn = new Button(task.getStatus() == Task.Status.COMPLETED ? "↶" : "✓");
            completeBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #10b981; -fx-border-radius: 50%; -fx-min-width: 20; -fx-min-height: 20; -fx-font-size: 10px;");
            completeBtn.setOnAction(e -> toggleTaskCompletion(task));

            Button editBtn = new Button("✎");
            editBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #667eea; -fx-border-radius: 50%; -fx-min-width: 20; -fx-min-height: 20; -fx-font-size: 10px;");
            editBtn.setOnAction(e -> {
                tasksList.getSelectionModel().select(task);
                loadTaskDetails(task);
            });

            quickActions.getChildren().addAll(completeBtn, editBtn);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            bottomRow.getChildren().addAll(spacer, quickActions);

            content.getChildren().addAll(titleRow, bottomRow);

            return content;
        } catch (Exception e) {
            System.err.println("Error creating task item: " + e.getMessage());
            // Fallback
            VBox fallback = new VBox();
            fallback.getChildren().add(new Label(task.getTitle()));
            return fallback;
        }
    }

    private String getPriorityIndicator(Task.Priority priority) {
        return switch (priority) {
            case HIGH -> "🔴";
            case MEDIUM -> "🟡";
            case LOW -> "🟢";
        };
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
            if (daysUntil <= 7) {
                return "In " + daysUntil + " days";
            } else {
                return dueDate.format(DateTimeFormatter.ofPattern("MMM dd"));
            }
        }
    }

    private void toggleTaskCompletion(Task task) {
        try {
            Task.Status newStatus = task.getStatus() == Task.Status.COMPLETED ?
                    Task.Status.PENDING : Task.Status.COMPLETED;

            task.setStatus(newStatus);

            // Update in background using JavaFX Task
            CompletableFuture.supplyAsync(() -> taskService.updateTask(task))
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            String message = newStatus == Task.Status.COMPLETED ?
                                    "Task completed! Great job! 🎉" : "Task marked as pending";

                            NotificationManager.getInstance().showNotification(
                                    "Task Updated", message, NotificationManager.NotificationType.SUCCESS
                            );

                            refreshTasksList();
                            updateStatistics();
                        } else {
                            // Revert the change
                            task.setStatus(newStatus == Task.Status.COMPLETED ?
                                    Task.Status.PENDING : Task.Status.COMPLETED);

                            ErrorHandler.handleError("Task Update", "Failed to update task status");
                        }
                    }));

        } catch (Exception e) {
            ErrorHandler.handleError("Task Toggle", "Failed to toggle task completion", e);
        }
    }

    private String getStatusStyle(Task.Status status) {
        return switch (status) {
            case COMPLETED -> "-fx-background-color: #dcfce7; -fx-text-fill: #166534;";
            case IN_PROGRESS -> "-fx-background-color: #dbeafe; -fx-text-fill: #1d4ed8;";
            case PENDING -> "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
        };
    }

    private String getTaskItemStyle(Task task) {
        if (task.getStatus() == Task.Status.COMPLETED) {
            return "-fx-background-color: #f0fdf4; -fx-background-radius: 8px; -fx-border-color: #bbf7d0; -fx-border-radius: 8px; -fx-border-width: 1;";
        } else if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now())) {
            return "-fx-background-color: #fef2f2; -fx-background-radius: 8px; -fx-border-color: #fecaca; -fx-border-radius: 8px; -fx-border-width: 1;";
        } else if (task.getPriority() == Task.Priority.HIGH) {
            return "-fx-background-color: #fff7ed; -fx-background-radius: 8px; -fx-border-color: #fed7aa; -fx-border-radius: 8px; -fx-border-width: 1;";
        }
        return "-fx-background-color: #f8fafc; -fx-background-radius: 8px; -fx-border-color: #e2e8f0; -fx-border-radius: 8px; -fx-border-width: 1;";
    }

    private void setupComboBoxes() {
        try {
            priorityCombo.getItems().addAll(Task.Priority.values());
            statusCombo.getItems().addAll(Task.Status.values());

            filterPriorityCombo.getItems().add(null); // For "All" option
            filterPriorityCombo.getItems().addAll(Task.Priority.values());
            filterPriorityCombo.setPromptText("All Priorities");

            filterStatusCombo.getItems().add(null); // For "All" option
            filterStatusCombo.getItems().addAll(Task.Status.values());
            filterStatusCombo.setPromptText("All Statuses");

            sortCombo.getItems().addAll(
                    "Priority (High-Low)", "Priority (Low-High)",
                    "Due Date (Earliest)", "Due Date (Latest)",
                    "Title (A-Z)", "Title (Z-A)",
                    "Created Date (Newest)", "Created Date (Oldest)",
                    "Status"
            );
            sortCombo.setValue("Priority (High-Low)");

            System.out.println("✅ Combo boxes setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Combo Setup", "Failed to setup combo boxes", e);
        }
    }

    private void setupButtons() {
        try {
            if (newTaskButton != null) {
                newTaskButton.setOnAction(e -> createNewTask());
                addButtonHoverEffect(newTaskButton);
            }

            if (saveTaskButton != null) {
                saveTaskButton.setOnAction(e -> saveCurrentTaskWithValidation());
                addButtonHoverEffect(saveTaskButton);
            }

            if (deleteTaskButton != null) {
                deleteTaskButton.setOnAction(e -> deleteCurrentTaskWithConfirmation());
                addButtonHoverEffect(deleteTaskButton);
            }

            System.out.println("✅ Buttons setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Buttons Setup", "Failed to setup buttons", e);
        }
    }

    private void addButtonHoverEffect(Button button) {
        try {
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
            scaleUp.setToX(1.05);
            scaleUp.setToY(1.05);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);

            button.setOnMouseEntered(e -> scaleUp.play());
            button.setOnMouseExited(e -> scaleDown.play());
        } catch (Exception e) {
            System.err.println("Warning: Could not add hover effect to button: " + e.getMessage());
        }
    }

    private void setupFilters() {
        try {
            if (searchField != null) {
                searchField.textProperty().addListener((obs, oldText, newText) -> applyFilters());
            }
            if (filterPriorityCombo != null) {
                filterPriorityCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
            }
            if (filterStatusCombo != null) {
                filterStatusCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
            }
            if (sortCombo != null) {
                sortCombo.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
            }
            if (showCompletedCheckBox != null) {
                showCompletedCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> applyFilters());
            }

            System.out.println("✅ Filters setup completed");
        } catch (Exception e) {
            ErrorHandler.handleError("Filters Setup", "Failed to setup filters", e);
        }
    }

    private void setupAutoSave() {
        try {
            // Auto-save when fields change (with debouncing)
            if (taskTitleField != null) {
                taskTitleField.textProperty().addListener((obs, oldText, newText) -> scheduleAutoSave());
            }
            if (taskDescriptionArea != null) {
                taskDescriptionArea.textProperty().addListener((obs, oldText, newText) -> scheduleAutoSave());
            }

            System.out.println("✅ Auto-save setup completed");
        } catch (Exception e) {
            System.err.println("Warning: Could not setup auto-save: " + e.getMessage());
        }
    }

    private void scheduleAutoSave() {
        try {
            if (autoSaveTimeline != null) {
                autoSaveTimeline.stop();
            }

            autoSaveTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                if (currentTask != null && taskTitleField != null && !taskTitleField.getText().trim().isEmpty()) {
                    saveCurrentTaskSilently();
                }
            }));
            autoSaveTimeline.play();
        } catch (Exception e) {
            System.err.println("Error scheduling auto-save: " + e.getMessage());
        }
    }

    private void setupKeyboardShortcuts() {
        try {
            // Add keyboard shortcuts for common actions
            if (tasksContainer != null) {
                tasksContainer.setOnKeyPressed(e -> {
                    switch (e.getCode()) {
                        case N:
                            if (e.isControlDown()) {
                                createNewTask();
                                e.consume();
                            }
                            break;
                        case S:
                            if (e.isControlDown()) {
                                saveCurrentTaskWithValidation();
                                e.consume();
                            }
                            break;
                        case DELETE:
                            if (currentTask != null) {
                                deleteCurrentTaskWithConfirmation();
                                e.consume();
                            }
                            break;
                    }
                });
            }

            System.out.println("✅ Keyboard shortcuts setup completed");
        } catch (Exception e) {
            System.err.println("Warning: Could not setup keyboard shortcuts: " + e.getMessage());
        }
    }

    private void addEntranceAnimation() {
        try {
            if (tasksContainer != null) {
                tasksContainer.setOpacity(0);
                tasksContainer.setTranslateY(20);

                FadeTransition fade = new FadeTransition(Duration.millis(600), tasksContainer);
                fade.setFromValue(0);
                fade.setToValue(1);

                TranslateTransition slide = new TranslateTransition(Duration.millis(600), tasksContainer);
                slide.setFromY(20);
                slide.setToY(0);
                slide.setInterpolator(Interpolator.EASE_OUT);

                ParallelTransition entrance = new ParallelTransition(fade, slide);
                entrance.play();
            }
        } catch (Exception e) {
            // Fallback: just show the container
            if (tasksContainer != null) {
                tasksContainer.setOpacity(1);
                tasksContainer.setTranslateY(0);
            }
        }
    }

    // ✅ TAMBAH METHOD BARU
    private void showEmptyState() {
        if (tasksList != null) {
            tasksList.getItems().clear();
            Label emptyLabel = new Label("Create your first task to get started!");
            emptyLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px; -fx-padding: 20;");
            tasksList.setPlaceholder(emptyLabel);
        }

        updateStatisticsLabels(0, 0, 0, 0);
    }

    private void loadTasksSafely() {
        if (taskService == null) {
            System.err.println("Task service not available");
            showEmptyState(); // ✅ TAMBAH: Method ini
            return;
        }

        try {
            CompletableFuture.supplyAsync(() -> {
                try {
                    int userId = UserSession.getInstance().getCurrentUser().getId();
                    return taskService.getTasksForUser(userId);
                } catch (Exception e) {
                    System.err.println("Error loading tasks: " + e.getMessage());
                    return List.<Task>of();
                }
            }).thenAccept(tasks -> {
                Platform.runLater(() -> {
                    try {
                        allTasks.setAll(tasks);
                        applyFilters();
                        updateStatistics();

                        if (tasks.isEmpty()) {
                            showEmptyState(); // ✅ TAMBAH
                        }

                        System.out.println("✅ Tasks loaded successfully: " + tasks.size() + " tasks");
                    } catch (Exception e) {
                        ErrorHandler.handleError("Tasks Loading", "Failed to display loaded tasks", e);
                        showEmptyState(); // ✅ TAMBAH
                    }
                });
            });
        } catch (Exception e) {
            ErrorHandler.handleError("Tasks Loading", "Failed to load tasks", e);
            showEmptyState(); // ✅ TAMBAH
        }
    }

    private void refreshTasksList() {
        loadTasksSafely();
    }

    private void applyFilters() {
        try {
            List<Task> filteredTasks = allTasks.stream()
                    .filter(task -> {
                        // Search filter
                        String searchText = searchField != null ? searchField.getText() : "";
                        if (searchText != null && !searchText.trim().isEmpty()) {
                            String search = searchText.toLowerCase();
                            if (!task.getTitle().toLowerCase().contains(search) &&
                                    (task.getDescription() == null || !task.getDescription().toLowerCase().contains(search))) {
                                return false;
                            }
                        }

                        // Priority filter
                        Task.Priority priorityFilter = filterPriorityCombo != null ? filterPriorityCombo.getValue() : null;
                        if (priorityFilter != null && task.getPriority() != priorityFilter) {
                            return false;
                        }

                        // Status filter
                        Task.Status statusFilter = filterStatusCombo != null ? filterStatusCombo.getValue() : null;
                        if (statusFilter != null && task.getStatus() != statusFilter) {
                            return false;
                        }

                        // Show completed filter
                        boolean showCompleted = showCompletedCheckBox != null ? showCompletedCheckBox.isSelected() : true;
                        if (!showCompleted && task.getStatus() == Task.Status.COMPLETED) {
                            return false;
                        }

                        return true;
                    })
                    .collect(Collectors.toList());

            // Apply sorting
            String sortOption = sortCombo != null ? sortCombo.getValue() : "Priority (High-Low)";
            if (sortOption != null) {
                switch (sortOption) {
                    case "Title (A-Z)" -> filteredTasks.sort((t1, t2) -> t1.getTitle().compareToIgnoreCase(t2.getTitle()));
                    case "Title (Z-A)" -> filteredTasks.sort((t1, t2) -> t2.getTitle().compareToIgnoreCase(t1.getTitle()));
                    case "Priority (High-Low)" -> filteredTasks.sort((t1, t2) -> t2.getPriority().compareTo(t1.getPriority()));
                    case "Priority (Low-High)" -> filteredTasks.sort((t1, t2) -> t1.getPriority().compareTo(t2.getPriority()));
                    case "Due Date (Earliest)" -> filteredTasks.sort((t1, t2) -> {
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return 1;
                        if (t2.getDueDate() == null) return -1;
                        return t1.getDueDate().compareTo(t2.getDueDate());
                    });
                    case "Due Date (Latest)" -> filteredTasks.sort((t1, t2) -> {
                        if (t1.getDueDate() == null && t2.getDueDate() == null) return 0;
                        if (t1.getDueDate() == null) return -1;
                        if (t2.getDueDate() == null) return 1;
                        return t2.getDueDate().compareTo(t1.getDueDate());
                    });
                    case "Created Date (Newest)" -> filteredTasks.sort((t1, t2) -> {
                        if (t1.getCreatedAt() == null && t2.getCreatedAt() == null) return 0;
                        if (t1.getCreatedAt() == null) return 1;
                        if (t2.getCreatedAt() == null) return -1;
                        return t2.getCreatedAt().compareTo(t1.getCreatedAt());
                    });
                    case "Created Date (Oldest)" -> filteredTasks.sort((t1, t2) -> {
                        if (t1.getCreatedAt() == null && t2.getCreatedAt() == null) return 0;
                        if (t1.getCreatedAt() == null) return -1;
                        if (t2.getCreatedAt() == null) return 1;
                        return t1.getCreatedAt().compareTo(t2.getCreatedAt());
                    });
                    case "Status" -> filteredTasks.sort((t1, t2) -> t1.getStatus().compareTo(t2.getStatus()));
                }
            }

            if (tasksList != null) {
                tasksList.getItems().setAll(filteredTasks);
            }

        } catch (Exception e) {
            ErrorHandler.handleError("Filter Application", "Failed to apply filters", e);
        }
    }

    private void loadTaskDetails(Task task) {
        try {
            currentTask = task;

            if (taskTitleField != null) taskTitleField.setText(task.getTitle());
            if (taskDescriptionArea != null) taskDescriptionArea.setText(task.getDescription());
            if (priorityCombo != null) priorityCombo.setValue(task.getPriority());
            if (statusCombo != null) statusCombo.setValue(task.getStatus());
            if (dueDatePicker != null) dueDatePicker.setValue(task.getDueDate());

            // Add form animation
            addFormChangeAnimation();

        } catch (Exception e) {
            ErrorHandler.handleError("Task Loading", "Failed to load task details", e);
        }
    }

    private void addFormChangeAnimation() {
        try {
            if (taskTitleField != null && taskTitleField.getParent() != null && taskTitleField.getParent().getParent() instanceof VBox) {
                VBox formContainer = (VBox) taskTitleField.getParent().getParent();
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), formContainer);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.02);
                pulse.setToY(1.02);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        } catch (Exception e) {
            // Ignore animation errors
        }
    }

    private void createNewTask() {
        try {
            currentTask = null;

            if (taskTitleField != null) taskTitleField.clear();
            if (taskDescriptionArea != null) taskDescriptionArea.clear();
            if (priorityCombo != null) priorityCombo.setValue(Task.Priority.MEDIUM);
            if (statusCombo != null) statusCombo.setValue(Task.Status.PENDING);
            if (dueDatePicker != null) dueDatePicker.setValue(null);

            if (tasksList != null) tasksList.getSelectionModel().clearSelection();

            // Focus on title field
            if (taskTitleField != null) {
                Platform.runLater(() -> taskTitleField.requestFocus());
            }

            // Add creation animation
            addFormChangeAnimation();

        } catch (Exception e) {
            ErrorHandler.handleError("New Task", "Failed to create new task form", e);
        }
    }

    private void saveCurrentTaskWithValidation() {
        try {
            String title = taskTitleField != null ? taskTitleField.getText().trim() : "";

            if (title.isEmpty()) {
                showValidationError("Task title cannot be empty");
                if (taskTitleField != null) {
                    taskTitleField.requestFocus();
                    addFieldErrorAnimation(taskTitleField);
                }
                return;
            }

            if (title.length() > 200) {
                showValidationError("Task title is too long (maximum 200 characters)");
                if (taskTitleField != null) {
                    taskTitleField.requestFocus();
                    addFieldErrorAnimation(taskTitleField);
                }
                return;
            }

            saveCurrentTask();

        } catch (Exception e) {
            ErrorHandler.handleError("Task Validation", "Failed to validate task", e);
        }
    }

    private void saveCurrentTask() {
        try {
            String title = taskTitleField != null ? taskTitleField.getText().trim() : "";
            String description = taskDescriptionArea != null ? taskDescriptionArea.getText().trim() : "";
            Task.Priority priority = priorityCombo != null ? priorityCombo.getValue() : Task.Priority.MEDIUM;
            Task.Status status = statusCombo != null ? statusCombo.getValue() : Task.Status.PENDING;
            LocalDate dueDate = dueDatePicker != null ? dueDatePicker.getValue() : null;

            Task task;
            if (currentTask == null) {
                task = new Task(title, description, priority, dueDate);
                task.setUserId(UserSession.getInstance().getCurrentUser().getId());
            } else {
                task = currentTask;
                task.setTitle(title);
                task.setDescription(description);
                task.setPriority(priority);
                task.setDueDate(dueDate);
            }

            task.setStatus(status);

            // Save in background
            CompletableFuture.supplyAsync(() -> {
                if (currentTask == null) {
                    return taskService.addTask(task);
                } else {
                    return taskService.updateTask(task);
                }
            }).thenAccept(success -> Platform.runLater(() -> {
                if (success) {
                    String action = currentTask == null ? "created" : "updated";
                    NotificationManager.getInstance().showNotification(
                            "Task Saved",
                            "Your task has been " + action + " successfully! 🎉",
                            NotificationManager.NotificationType.SUCCESS
                    );

                    currentTask = task; // Update current task reference
                    refreshTasksList();
                    updateStatistics();

                    // Add success animation
                    addSuccessAnimation();
                } else {
                    NotificationManager.getInstance().showNotification(
                            "Error",
                            "Failed to save task. Please try again.",
                            NotificationManager.NotificationType.ERROR
                    );
                }
            }));

        } catch (Exception e) {
            ErrorHandler.handleError("Task Save", "Failed to save task", e);
        }
    }

    private void saveCurrentTaskSilently() {
        try {
            if (currentTask != null && taskTitleField != null && !taskTitleField.getText().trim().isEmpty()) {
                String title = taskTitleField.getText().trim();
                String description = taskDescriptionArea != null ? taskDescriptionArea.getText().trim() : "";

                currentTask.setTitle(title);
                currentTask.setDescription(description);

                // Save silently in background
                CompletableFuture.runAsync(() -> taskService.updateTask(currentTask));
            }
        } catch (Exception e) {
            System.err.println("Silent save failed: " + e.getMessage());
        }
    }

    private void deleteCurrentTaskWithConfirmation() {
        if (currentTask == null) {
            showValidationError("No task selected for deletion");
            return;
        }

        try {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Task");
            alert.setHeaderText("Are you sure you want to delete this task?");
            alert.setContentText("Task: \"" + currentTask.getTitle() + "\"\n\nThis action cannot be undone.");

            // Style the alert
            alert.getDialogPane().getStylesheets().addAll(
                    tasksContainer.getScene().getStylesheets());

            alert.showAndWait().ifPresent(result -> {
                if (result == ButtonType.OK) {
                    deleteCurrentTask();
                }
            });

        } catch (Exception e) {
            ErrorHandler.handleError("Delete Confirmation", "Failed to show delete confirmation", e);
        }
    }

    private void deleteCurrentTask() {
        try {
            Task taskToDelete = currentTask;

            // Delete in background
            CompletableFuture.supplyAsync(() -> taskService.deleteTask(taskToDelete.getId()))
                    .thenAccept(success -> Platform.runLater(() -> {
                        if (success) {
                            NotificationManager.getInstance().showNotification(
                                    "Task Deleted",
                                    "Task has been deleted successfully",
                                    NotificationManager.NotificationType.SUCCESS
                            );

                            refreshTasksList();
                            updateStatistics();
                            createNewTask();

                            // Add deletion animation
                            addDeletionAnimation();
                        } else {
                            NotificationManager.getInstance().showNotification(
                                    "Error",
                                    "Failed to delete task. Please try again.",
                                    NotificationManager.NotificationType.ERROR
                            );
                        }
                    }));

        } catch (Exception e) {
            ErrorHandler.handleError("Task Deletion", "Failed to delete task", e);
        }
    }

    private void updateStatistics() {
        try {
            if (allTasks.isEmpty()) {
                updateStatisticsLabels(0, 0, 0, 0);
                return;
            }

            int total = allTasks.size();
            int pending = (int) allTasks.stream().filter(t -> t.getStatus() == Task.Status.PENDING).count();
            int completed = (int) allTasks.stream().filter(t -> t.getStatus() == Task.Status.COMPLETED).count();
            int overdue = (int) allTasks.stream()
                    .filter(t -> t.getDueDate() != null &&
                            t.getDueDate().isBefore(LocalDate.now()) &&
                            t.getStatus() != Task.Status.COMPLETED)
                    .count();

            updateStatisticsLabels(total, pending, completed, overdue);

        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    private void updateStatisticsLabels(int total, int pending, int completed, int overdue) {
        try {
            // ✅ UBAH: Tampilkan pesan yang lebih user-friendly
            if (totalTasksLabel != null) {
                totalTasksLabel.setText(total > 0 ? "Total: " + total : "No tasks yet");
            }
            if (pendingTasksLabel != null) {
                pendingTasksLabel.setText(pending > 0 ? "Pending: " + pending : "No pending tasks");
            }
            if (completedTasksLabel != null) {
                completedTasksLabel.setText(completed > 0 ? "Completed: " + completed : "No completed tasks");
            }
            if (overdueTasksLabel != null) {
                overdueTasksLabel.setText(overdue > 0 ? "Overdue: " + overdue : "No overdue tasks");
                if (overdue > 0) {
                    overdueTasksLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                } else {
                    overdueTasksLabel.setStyle("");
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics labels: " + e.getMessage());
        }
    }

    private void showValidationError(String message) {
        try {
            NotificationManager.getInstance().showNotification(
                    "Validation Error",
                    message,
                    NotificationManager.NotificationType.WARNING
            );
        } catch (Exception e) {
            System.err.println("Error showing validation error: " + e.getMessage());
        }
    }

    private void addFieldErrorAnimation(Control field) {
        try {
            // Red border flash
            String originalStyle = field.getStyle();
            field.setStyle(originalStyle + " -fx-border-color: #ef4444; -fx-border-width: 2;");

            Timeline resetStyle = new Timeline(new KeyFrame(Duration.seconds(2),
                    e -> field.setStyle(originalStyle)));
            resetStyle.play();

            // Shake animation
            TranslateTransition shake = new TranslateTransition(Duration.millis(50), field);
            shake.setFromX(0);
            shake.setByX(5);
            shake.setCycleCount(6);
            shake.setAutoReverse(true);
            shake.setOnFinished(e -> field.setTranslateX(0));
            shake.play();
        } catch (Exception e) {
            System.err.println("Error adding field error animation: " + e.getMessage());
        }
    }

    private void addSuccessAnimation() {
        try {
            if (saveTaskButton != null) {
                // Success pulse
                ScaleTransition pulse = new ScaleTransition(Duration.millis(150), saveTaskButton);
                pulse.setFromX(1.0);
                pulse.setFromY(1.0);
                pulse.setToX(1.1);
                pulse.setToY(1.1);
                pulse.setCycleCount(2);
                pulse.setAutoReverse(true);
                pulse.play();
            }
        } catch (Exception e) {
            System.err.println("Error adding success animation: " + e.getMessage());
        }
    }

    private void addDeletionAnimation() {
        try {
            if (deleteTaskButton != null) {
                // Deletion flash
                FadeTransition flash = new FadeTransition(Duration.millis(100), deleteTaskButton);
                flash.setFromValue(1.0);
                flash.setToValue(0.3);
                flash.setCycleCount(4);
                flash.setAutoReverse(true);
                flash.play();
            }
        } catch (Exception e) {
            System.err.println("Error adding deletion animation: " + e.getMessage());
        }
    }
}